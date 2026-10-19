package io.github.vinaygaykar.mp1.playback

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.data.local.scanner.ScannerRepository
import io.github.vinaygaykar.mp1.data.repo.SettingsRepository
import io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType
import io.github.vinaygaykar.mp1.domain.models.VisualizerState
import io.github.vinaygaykar.mp1.domain.repository.AudioVisualizerRepository
import io.github.vinaygaykar.mp1.domain.repository.LyricsRepository
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsRepository: LyricsRepository,
    private val musicRepository: MusicRepository,
    private val scannerRepository: ScannerRepository,
    private val settingsRepository: SettingsRepository,
    private val audioVisualizerRepository: AudioVisualizerRepository
) : ViewModel(), PlayerActions {

    private var controller: MediaController? = null
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _visualizerState = MutableStateFlow<VisualizerState>(VisualizerState.None)
    val visualizerState: StateFlow<VisualizerState> = _visualizerState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var currentArtworkJob: Job? = null
    private var visualizerJob: Job? = null
    private var activeVisualizerSongId: String? = null
    private var activeVisualizerWave: PlaybackWaveType = PlaybackWaveType.ENERGY
    private var isVisualizerPlayerVisible: Boolean = false
    private var queueUpdateJob: Job? = null
    private var activeArtworkUri: Uri? = null
    private var activeMediaId: String? = null

    @Volatile
    private var currentWindowIndices: List<Int> = emptyList()

    init {
        viewModelScope.launch {
            scannerRepository.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanInProgress = isScanning) }
            }
        }
        viewModelScope.launch {
            settingsRepository.playbackWave.collect { wave ->
                _uiState.update { it.copy(playbackWave = wave) }
                triggerVisualizerExtractionIfNeeded()
            }
        }

        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
            controller?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateState()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState()
                    if (isPlaying) startPositionUpdates() else stopPositionUpdates()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateState()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    updateState()
                    controller?.currentTimeline?.let { updateQueueFromTimeline(it) }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    updateState()
                }

                override fun onTimelineChanged(
                    timeline: Timeline,
                    reason: Int
                ) {
                    updateState()
                    updateQueueFromTimeline(timeline)
                }
            })
            updateState()
            controller?.currentTimeline?.let { updateQueueFromTimeline(it) }
            if (controller?.isPlaying == true) {
                startPositionUpdates()
            }
        }
    }

    private fun updateState() {
        val player = controller ?: return
        val currentItem = player.currentMediaItem

        if (currentItem == null) {
            activeArtworkUri = null
            activeMediaId = null
            currentArtworkJob?.cancel()
            _uiState.update { PlayerUiState(isScanInProgress = it.isScanInProgress, playbackWave = it.playbackWave) }
            return
        }

        val mediaId = currentItem.mediaId
        val extras = currentItem.mediaMetadata.extras
        val albumIdFromExtras = extras?.getString("album_id")

        val isNewMediaItem = mediaId != activeMediaId
        if (isNewMediaItem) {
            activeMediaId = mediaId
        }

        val albumIdToUse =
            if (isNewMediaItem) albumIdFromExtras else (_uiState.value.albumId ?: albumIdFromExtras)

        val artworkUri = currentItem.mediaMetadata.artworkUri
        val isNewArtwork = artworkUri != activeArtworkUri

        if (isNewArtwork) {
            activeArtworkUri = artworkUri
            if (artworkUri != null) {
                fetchSeedColor(artworkUri)
            } else {
                currentArtworkJob?.cancel()
            }
        }

        val seedColorToUse = if (isNewArtwork) null else _uiState.value.seedColor

        val lyricsToUse: String?
        val isSyncedToUse: Boolean
        val isLocalToUse: Boolean
        val lyricsLoadingToUse: Boolean
        val lyricsErrorToUse: String?

        if (isNewMediaItem) {
            if (mediaId != activeVisualizerSongId) {
                visualizerJob?.cancel()
                visualizerJob = null
                audioVisualizerRepository.clearCache()
                _visualizerState.value = VisualizerState.None
                activeVisualizerSongId = null
            }
            lyricsToUse = null
            isSyncedToUse = false
            isLocalToUse = false
            lyricsLoadingToUse = false
            lyricsErrorToUse = null
            if (isVisualizerPlayerVisible) {
                triggerVisualizerExtractionIfNeeded()
            }
        } else {
            lyricsToUse = _uiState.value.lyrics
            isSyncedToUse = _uiState.value.isLyricsSynced
            isLocalToUse = _uiState.value.isLyricsLocal
            lyricsLoadingToUse = _uiState.value.lyricsLoading
            lyricsErrorToUse = _uiState.value.lyricsError
        }

        val currentWindowIndex = player.currentMediaItemIndex
        val windowIndices = currentWindowIndices
        val uiCurrentIndex = if (windowIndices.isNotEmpty()) {
            val found = windowIndices.indexOf(currentWindowIndex)
            if (found != -1) found else currentWindowIndex.coerceIn(0, (windowIndices.size - 1).coerceAtLeast(0))
        } else {
            currentWindowIndex
        }

        _uiState.update {
            it.copy(
                trackTitle = currentItem.mediaMetadata.title?.toString() ?: "Unknown",
                artist = currentItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                album = currentItem.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                albumId = albumIdToUse,
                artworkUri = artworkUri,
                seedColor = seedColorToUse,
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L),
                isPlaying = player.isPlaying,
                isShuffleOn = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
                lyrics = lyricsToUse,
                isLyricsSynced = isSyncedToUse,
                isLyricsLocal = isLocalToUse,
                lyricsLoading = lyricsLoadingToUse,
                lyricsError = lyricsErrorToUse,
                queue = _uiState.value.queue,
                currentIndex = uiCurrentIndex
            )
        }

        if (albumIdToUse.isNullOrEmpty() && mediaId.isNotEmpty()) {
            viewModelScope.launch {
                val song = musicRepository.getSongById(mediaId)
                val resolvedAlbumId = song?.albumId ?: run {
                    val currentAlbumName = currentItem.mediaMetadata.albumTitle?.toString()
                    if (!currentAlbumName.isNullOrEmpty()) {
                        musicRepository.getAllAlbums().firstOrNull()?.find {
                            it.name.equals(currentAlbumName, ignoreCase = true)
                        }?.id
                    } else null
                }
                if (!resolvedAlbumId.isNullOrEmpty()) {
                    _uiState.update { it.copy(albumId = resolvedAlbumId) }
                }
            }
        }

        if (lyricsToUse == null && !lyricsLoadingToUse && lyricsErrorToUse == null) {
            fetchLyrics(mediaId)
        }
    }

    private fun updateQueueFromTimeline(timeline: Timeline) {
        if (timeline.isEmpty) {
            queueUpdateJob?.cancel()
            currentWindowIndices = emptyList()
            _uiState.update { it.copy(queue = persistentListOf(), currentIndex = -1) }
            return
        }
        queueUpdateJob?.cancel()
        queueUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce rapid consecutive timeline changes during chunk loading
            delay(200)
            val count = timeline.windowCount
            if (count == 0) {
                currentWindowIndices = emptyList()
                _uiState.update { it.copy(queue = persistentListOf(), currentIndex = -1) }
                return@launch
            }

            val (isShuffleOn, currentWindowIndex) = withContext(Dispatchers.Main) {
                val p = controller
                (p?.shuffleModeEnabled ?: false) to (p?.currentMediaItemIndex ?: -1)
            }

            val window = Timeline.Window()
            val newQueue = ArrayList<MediaItem>(count)
            val newWindowIndices = ArrayList<Int>(count)

            if (isShuffleOn && count > 1) {
                val visited = BooleanArray(count)
                var visitedCount = 0

                // Anchor shuffle queue at currentWindowIndex so the playing track is always first
                var index = if (currentWindowIndex in 0 until count) {
                    currentWindowIndex
                } else {
                    timeline.getFirstWindowIndex(true)
                }

                while (index in 0 until count && !visited[index] && visitedCount < count) {
                    visited[index] = true
                    timeline.getWindow(index, window)
                    newQueue.add(window.mediaItem)
                    newWindowIndices.add(index)
                    visitedCount++
                    index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
                }

                if (visitedCount < count) {
                    var fallbackIndex = timeline.getFirstWindowIndex(true)
                    while (fallbackIndex in 0 until count && visitedCount < count) {
                        if (!visited[fallbackIndex]) {
                            visited[fallbackIndex] = true
                            timeline.getWindow(fallbackIndex, window)
                            newQueue.add(window.mediaItem)
                            newWindowIndices.add(fallbackIndex)
                            visitedCount++
                        }
                        fallbackIndex = timeline.getNextWindowIndex(fallbackIndex, Player.REPEAT_MODE_OFF, true)
                    }
                }

                if (visitedCount < count) {
                    for (i in 0 until count) {
                        if (!visited[i]) {
                            timeline.getWindow(i, window)
                            newQueue.add(window.mediaItem)
                            newWindowIndices.add(i)
                        }
                    }
                }
            } else {
                for (i in 0 until count) {
                    timeline.getWindow(i, window)
                    newQueue.add(window.mediaItem)
                    newWindowIndices.add(i)
                }
            }

            currentWindowIndices = newWindowIndices
            val uiCurrentIndex = if (currentWindowIndex >= 0 && newWindowIndices.isNotEmpty()) {
                val found = newWindowIndices.indexOf(currentWindowIndex)
                if (found != -1) found else currentWindowIndex.coerceIn(0, newWindowIndices.size - 1)
            } else {
                currentWindowIndex
            }

            _uiState.update {
                it.copy(
                    queue = newQueue.toPersistentList(),
                    currentIndex = uiCurrentIndex,
                    isShuffleOn = isShuffleOn
                )
            }
        }
    }

    private fun fetchSeedColor(uri: Uri) {
        currentArtworkJob?.cancel()
        currentArtworkJob = viewModelScope.launch {
            delay(200)
            val extractedColor = DynamicThemeHelper.extractSeedColor(context, uri)
            if (activeArtworkUri == uri) {
                _uiState.update { it.copy(seedColor = extractedColor) }
            }
        }
    }

    private fun fetchLyrics(songId: String) {
        viewModelScope.launch {
            val song = musicRepository.getSongById(songId) ?: return@launch

            // Avoid refetching if already loading or if we have lyrics
            if (_uiState.value.lyrics != null || _uiState.value.lyricsLoading) return@launch

            _uiState.update { it.copy(lyricsLoading = true, lyricsError = null) }

            lyricsRepository.getLyrics(song)
                .onSuccess { lyrics ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        val isLocal = isLocalOrEmbeddedLyrics(context, song.uri.toString(), song.fileName, lyrics.content)
                        _uiState.update {
                            it.copy(
                                lyrics = lyrics.content,
                                isLyricsSynced = lyrics.isSynced,
                                isLyricsLocal = isLocal,
                                lyricsLoading = false
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        _uiState.update {
                            it.copy(
                                lyricsLoading = false,
                                lyricsError = error.message
                            )
                        }
                    }
                }
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                updatePosition()
                delay(200)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun updatePosition() {
        val player = controller ?: return
        _uiState.update { it.copy(positionMs = player.currentPosition) }
    }

    override fun onPlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun stop() {
        controller?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
    }

    override fun onNext() {
        controller?.seekToNextMediaItem()
    }

    override fun onPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    override fun onSeek(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun onShuffleToggle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    override fun onRepeatToggle() {
        controller?.let {
            it.repeatMode = (it.repeatMode + 1) % 3
        }
    }

    override fun onLyricsSearch(query: String) {
        val currentItem = controller?.currentMediaItem ?: return
        val songId = currentItem.mediaId

        viewModelScope.launch {
            _uiState.update { it.copy(lyricsLoading = true, lyricsError = null) }

            lyricsRepository.searchLyrics(query, songId)
                .onSuccess { lyrics ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        val song = musicRepository.getSongById(songId)
                        val isLocal = if (song != null) {
                            isLocalOrEmbeddedLyrics(context, song.uri.toString(), song.fileName, lyrics.content)
                        } else false
                        _uiState.update {
                            it.copy(
                                lyrics = lyrics.content,
                                isLyricsSynced = lyrics.isSynced,
                                isLyricsLocal = isLocal,
                                lyricsLoading = false
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        _uiState.update {
                            it.copy(
                                lyricsLoading = false,
                                lyricsError = error.message
                            )
                        }
                    }
                }
        }
    }

    private fun isLocalOrEmbeddedLyrics(
        context: Context,
        songUriString: String,
        fileName: String,
        lyricsContent: String?
    ): Boolean {
        if (lyricsContent.isNullOrBlank()) return false

        val uri = try { Uri.parse(songUriString) } catch (_: Exception) { null } ?: return false

        val lrcFileName = if (fileName.contains(".")) {
            fileName.substringBeforeLast(".") + ".lrc"
        } else {
            "$fileName.lrc"
        }

        try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                val parent = file.parentFile
                if (parent != null && File(parent, lrcFileName).exists()) {
                    return true
                }
            } else if (uri.scheme == "content") {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val lrcDocId = if (docId.contains(".")) {
                        docId.substringBeforeLast(".") + ".lrc"
                    } else {
                        "$docId.lrc"
                    }
                    val lrcUri = DocumentsContract.buildDocumentUriUsingTree(uri, lrcDocId)
                    val exists = context.contentResolver.openInputStream(lrcUri)?.use { true } ?: false
                    if (exists) return true
                }

                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx != -1) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val audioFile = File(path)
                                val parent = audioFile.parentFile
                                if (parent != null && parent.exists()) {
                                    if (File(parent, lrcFileName).exists()) {
                                        return true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val retriever = MediaMetadataRetriever()
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                var embedded: String? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    embedded = retriever.extractMetadata(35) // METADATA_KEY_LYRICS
                }
                if (!embedded.isNullOrBlank()) {
                    retriever.release()
                    return true
                }
            }
            retriever.release()
        } catch (_: Exception) {}

        return false
    }

    override fun onSaveLyrics() {
        val currentItem = controller?.currentMediaItem ?: return
        val songId = currentItem.mediaId
        val lyricsContent = _uiState.value.lyrics ?: return

        viewModelScope.launch {
            val song = musicRepository.getSongById(songId)
            if (song != null) {
                saveLrcFileToStorage(context, song.uri.toString(), song.fileName, lyricsContent)
            }
            _uiState.update { it.copy(isLyricsLocal = true) }
        }
    }

    private fun saveLrcFileToStorage(context: Context, songUriString: String, fileName: String, lyricsContent: String): Boolean {
        return try {
            val uri = Uri.parse(songUriString)
            val lrcFileName = if (fileName.contains(".")) {
                fileName.substringBeforeLast(".") + ".lrc"
            } else {
                "$fileName.lrc"
            }

            if (uri.scheme == "file") {
                val file = File(uri.path ?: return false)
                val parent = file.parentFile ?: return false
                val lrcFile = File(parent, lrcFileName)
                lrcFile.writeText(lyricsContent)
                true
            } else if (uri.scheme == "content") {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val parentDocId = if (docId.contains("/")) docId.substringBeforeLast("/") else null
                    if (parentDocId != null) {
                        val treeUri = DocumentsContract.buildDocumentUriUsingTree(uri, parentDocId)
                        val parentFolderUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                        val newLrcUri = DocumentsContract.createDocument(
                            context.contentResolver,
                            parentFolderUri,
                            "*/*",
                            lrcFileName
                        )
                        if (newLrcUri != null) {
                            context.contentResolver.openOutputStream(newLrcUri)?.use { out ->
                                out.write(lyricsContent.toByteArray())
                            }
                            return true
                        }
                    }
                }
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx != -1) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val audioFile = File(path)
                                val parent = audioFile.parentFile
                                if (parent != null && parent.exists()) {
                                    val lrcFile = File(parent, lrcFileName)
                                    lrcFile.writeText(lyricsContent)
                                    return true
                                }
                            }
                        }
                    }
                }
                false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun addToQueue(mediaItem: MediaItem) {
        controller?.addMediaItem(mediaItem)
    }

    override fun onSkipToQueueItem(index: Int) {
        val player = controller ?: return
        val windowIndices = currentWindowIndices
        val targetWindowIndex = windowIndices.getOrNull(index) ?: index
        if (targetWindowIndex in 0 until player.mediaItemCount) {
            player.seekTo(targetWindowIndex, 0L)
            player.play()
        }
    }

    override fun removeFromQueue(index: Int) {
        val player = controller ?: return
        val windowIndices = currentWindowIndices
        val targetWindowIndex = windowIndices.getOrNull(index) ?: index
        if (targetWindowIndex in 0 until player.mediaItemCount) {
            player.removeMediaItem(targetWindowIndex)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        val windowIndices = currentWindowIndices
        val fromWindow = windowIndices.getOrNull(fromIndex) ?: fromIndex
        val toWindow = windowIndices.getOrNull(toIndex) ?: toIndex

        if (fromWindow in 0 until player.mediaItemCount && toWindow in 0 until player.mediaItemCount && fromWindow != toWindow) {
            if (player.shuffleModeEnabled) {
                player.shuffleModeEnabled = false
            }
            player.moveMediaItem(fromWindow, toWindow)
        }
    }

    override fun clearQueue() {
        val player = controller ?: return
        val count = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex
        if (count <= 1) return
        if (currentIndex in 0 until count) {
            if (currentIndex < count - 1) {
                player.removeMediaItems(currentIndex + 1, count)
            }
            if (currentIndex > 0) {
                player.removeMediaItems(0, currentIndex)
            }
        } else {
            player.clearMediaItems()
        }
    }

    override fun removeIndicesFromQueue(indices: Set<Int>) {
        val player = controller ?: return
        val windowIndices = currentWindowIndices
        val targetWindowIndices = indices.mapNotNull { windowIndices.getOrNull(it) }
            .filter { it in 0 until player.mediaItemCount }
            .sortedDescending()
        for (wIndex in targetWindowIndices) {
            player.removeMediaItem(wIndex)
        }
    }

    override fun removeOthersFromQueue(indicesToKeep: Set<Int>) {
        val player = controller ?: return
        val windowIndices = currentWindowIndices
        val targetWindowIndicesToKeep = indicesToKeep.mapNotNull { windowIndices.getOrNull(it) }.toSet()
        val totalCount = player.mediaItemCount
        val indicesToRemove = (0 until totalCount).filter { it !in targetWindowIndicesToKeep }.sortedDescending()
        for (wIndex in indicesToRemove) {
            player.removeMediaItem(wIndex)
        }
    }

    override fun playNextInQueue(indices: Set<Int>) {
        val player = controller ?: return
        val totalCount = player.mediaItemCount
        val currentIdx = player.currentMediaItemIndex
        if (totalCount <= 1 || indices.isEmpty()) return

        val windowIndices = currentWindowIndices
        val targetWindowIndices = indices.mapNotNull { windowIndices.getOrNull(it) }
            .filter { it in 0 until totalCount }.toSet()
        if (targetWindowIndices.isEmpty()) return

        if (player.shuffleModeEnabled) {
            player.shuffleModeEnabled = false
        }

        val activeCurrentIdx = if (currentIdx in 0 until totalCount) currentIdx else 0
        val selectedWithoutCurrent = targetWindowIndices.filter { it != activeCurrentIdx }.sorted()
        val unselected = (0 until totalCount).filter { it !in targetWindowIndices || it == activeCurrentIdx }

        val currentPosInUnselected = unselected.indexOf(activeCurrentIdx).let { if (it == -1) 0 else it }

        val targetList = mutableListOf<Int>()
        for (i in 0..currentPosInUnselected) {
            targetList.add(unselected[i])
        }
        targetList.addAll(selectedWithoutCurrent)
        for (i in (currentPosInUnselected + 1) until unselected.size) {
            targetList.add(unselected[i])
        }

        val workingList = (0 until totalCount).toMutableList()
        for (i in targetList.indices) {
            val targetVal = targetList[i]
            val fromIndex = (i until workingList.size).firstOrNull { workingList[it] == targetVal }
            if (fromIndex != null && fromIndex != i) {
                player.moveMediaItem(fromIndex, i)
                val item = workingList.removeAt(fromIndex)
                workingList.add(i, item)
            }
        }
        updateState()
    }

    override fun onUpdateVisualizerVisibility(isPlayerVisible: Boolean) {
        isVisualizerPlayerVisible = isPlayerVisible
        triggerVisualizerExtractionIfNeeded()
    }

    private fun triggerVisualizerExtractionIfNeeded() {
        val activeItem = controller?.currentMediaItem
        val activeId = activeItem?.mediaId?.takeIf { it.isNotEmpty() } ?: activeMediaId ?: ""
        val waveMode = _uiState.value.playbackWave

        if (activeId.isEmpty() || waveMode == PlaybackWaveType.SIMPLICITY) {
            if (activeVisualizerSongId != null) {
                visualizerJob?.cancel()
                visualizerJob = null
                audioVisualizerRepository.clearCache()
                _visualizerState.value = VisualizerState.None
                activeVisualizerSongId = null
                activeVisualizerWave = PlaybackWaveType.SIMPLICITY
            }
            return
        }

        if (!isVisualizerPlayerVisible) {
            if (activeVisualizerSongId != null && activeVisualizerSongId != activeId) {
                visualizerJob?.cancel()
                visualizerJob = null
                audioVisualizerRepository.clearCache()
                _visualizerState.value = VisualizerState.None
                activeVisualizerSongId = null
                activeVisualizerWave = PlaybackWaveType.SIMPLICITY
            }
            return
        }

        if (activeVisualizerSongId == activeId && activeVisualizerWave == waveMode && _visualizerState.value !is VisualizerState.None) {
            return
        }

        visualizerJob?.cancel()
        if (activeVisualizerSongId != activeId || activeVisualizerWave != waveMode) {
            audioVisualizerRepository.clearCache()
            _visualizerState.value = VisualizerState.None
        }

        activeVisualizerSongId = activeId
        activeVisualizerWave = waveMode

        visualizerJob = viewModelScope.launch(Dispatchers.IO) {
            val song = musicRepository.getSongById(activeId)
            val songUri = song?.uri ?: activeItem?.requestMetadata?.mediaUri ?: Uri.EMPTY
            if (songUri != Uri.EMPTY) {
                when (waveMode) {
                    PlaybackWaveType.ENERGY -> {
                        audioVisualizerRepository.getAudioEnergyFlow(activeId, songUri)
                            .catch { /* ignore */ }
                            .collect { energyCurve ->
                                _visualizerState.value = VisualizerState.Energy(energyCurve)
                            }
                    }
                    PlaybackWaveType.FREQUENCY -> {
                        audioVisualizerRepository.getAudioFrequencyFlow(activeId, songUri)
                            .catch { /* ignore */ }
                            .collect { frequencyCurve ->
                                _visualizerState.value = VisualizerState.Frequency(frequencyCurve)
                            }
                    }
                    PlaybackWaveType.SIMPLICITY -> {
                        _visualizerState.value = VisualizerState.None
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
