package io.github.vinaygaykar.mp1.playback

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.vinaygaykar.mp1.playback.client.PlaybackConnection
import io.github.vinaygaykar.mp1.playback.session.PlaybackSessionContract
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsRepository: LyricsRepository,
    private val musicRepository: MusicRepository,
    private val scannerRepository: ScannerRepository,
    private val settingsRepository: SettingsRepository,
    private val audioVisualizerRepository: AudioVisualizerRepository,
    private val queueManager: QueueManager,
    private val playbackConnection: PlaybackConnection
) : ViewModel(), PlaybackControls, QueueControls, LyricsControls {

    private var controller: MediaController? = null
    private var playerListener: Player.Listener? = null

    // Domain state flows
    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _progressState = MutableStateFlow(PlaybackProgressState())
    val progressState: StateFlow<PlaybackProgressState> = _progressState.asStateFlow()

    private val _queueState = MutableStateFlow(QueueUiState())
    val queueState: StateFlow<QueueUiState> = _queueState.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.None)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

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
                _playbackState.update { it.copy(isScanInProgress = isScanning) }
            }
        }
        viewModelScope.launch {
            settingsRepository.playbackWave.collect { wave ->
                _playbackState.update { it.copy(playbackWave = wave) }
                triggerVisualizerExtractionIfNeeded()
            }
        }

        playbackConnection.current.filterNotNull().onEach { newController ->
            stopPositionUpdates()
            playerListener?.let { controller?.removeListener(it) }
            controller = newController
            playerListener = object : Player.Listener {
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
                    controller?.currentTimeline?.let { updateQueueFromTimeline(it, forceRebuild = true) }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    updateState()
                }

                override fun onTimelineChanged(
                    timeline: Timeline,
                    reason: Int
                ) {
                    if (reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                        updateState()
                    }
                    val isPlaylistChanged = reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
                    updateQueueFromTimeline(timeline, forceRebuild = isPlaylistChanged)
                }
            }.also { newController.addListener(it) }
            updateState()
            controller?.currentTimeline?.let { updateQueueFromTimeline(it, forceRebuild = true) }
            if (controller?.isPlaying == true) {
                startPositionUpdates()
            }
        }.launchIn(viewModelScope)
    }

    private fun updateState() {
        val player = controller ?: return
        val currentItem = player.currentMediaItem

        if (currentItem == null) {
            activeArtworkUri = null
            activeMediaId = null
            currentArtworkJob?.cancel()
            _playbackState.update { PlaybackUiState(isScanInProgress = it.isScanInProgress, playbackWave = it.playbackWave) }
            _progressState.update { PlaybackProgressState() }
            _lyricsState.update { LyricsUiState.None }
            return
        }

        val mediaId = currentItem.mediaId
        val extras = currentItem.mediaMetadata.extras
        val albumIdFromExtras = extras?.getString(PlaybackSessionContract.EXTRA_ALBUM_ID)

        val isNewMediaItem = mediaId != activeMediaId
        if (isNewMediaItem) {
            activeMediaId = mediaId
        }

        val albumIdToUse =
            if (isNewMediaItem) albumIdFromExtras else (_playbackState.value.albumId ?: albumIdFromExtras)

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

        val seedColorToUse = _playbackState.value.seedColor

        if (isNewMediaItem) {
            if (mediaId != activeVisualizerSongId) {
                visualizerJob?.cancel()
                visualizerJob = null
                audioVisualizerRepository.clearCache()
                _visualizerState.value = VisualizerState.None
                activeVisualizerSongId = null
            }
            _lyricsState.update { LyricsUiState.None }
            if (isVisualizerPlayerVisible) {
                triggerVisualizerExtractionIfNeeded()
            }
        }

        val currentWindowIndex = player.currentMediaItemIndex
        val windowIndices = currentWindowIndices
        val uiCurrentIndex = if (windowIndices.isNotEmpty()) {
            val found = windowIndices.indexOf(currentWindowIndex)
            if (found != -1) found else currentWindowIndex.coerceIn(0, (windowIndices.size - 1).coerceAtLeast(0))
        } else {
            currentWindowIndex
        }

        _playbackState.update {
            it.copy(
                trackTitle = currentItem.mediaMetadata.title?.toString() ?: "Unknown",
                artist = currentItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                album = currentItem.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                albumId = albumIdToUse,
                artworkUri = artworkUri,
                seedColor = seedColorToUse,
                isPlaying = player.isPlaying,
                isShuffleOn = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem()
            )
        }

        _progressState.update {
            it.copy(
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }

        _queueState.update {
            it.copy(currentIndex = uiCurrentIndex)
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
                    _playbackState.update { it.copy(albumId = resolvedAlbumId) }
                }
            }
        }

        if (_lyricsState.value is LyricsUiState.None) {
            fetchLyrics(mediaId)
        }
    }

    private fun updateQueueFromTimeline(timeline: Timeline, forceRebuild: Boolean = false) {
        if (timeline.isEmpty) {
            queueUpdateJob?.cancel()
            currentWindowIndices = emptyList()
            _queueState.update { QueueUiState(items = persistentListOf(), currentIndex = -1) }
            return
        }
        queueUpdateJob?.cancel()
        queueUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            delay(200)

            val (isShuffleOn, currentWindowIndex) = withContext(Dispatchers.Main) {
                val p = controller
                (p?.shuffleModeEnabled ?: false) to (p?.currentMediaItemIndex ?: -1)
            }

            val result = queueManager.buildQueueFromTimeline(
                timeline = timeline,
                isShuffleOn = isShuffleOn,
                currentWindowIndex = currentWindowIndex,
                currentWindowIndices = currentWindowIndices,
                forceRebuild = forceRebuild || _playbackState.value.isShuffleOn != isShuffleOn
            )

            currentWindowIndices = result.windowIndices
            _playbackState.update { it.copy(isShuffleOn = result.isShuffleOn) }
            _queueState.update { current ->
                if (result.items.isNotEmpty()) {
                    QueueUiState(
                        items = result.items,
                        currentIndex = result.uiCurrentIndex
                    )
                } else {
                    current.copy(currentIndex = result.uiCurrentIndex)
                }
            }
        }
    }

    private fun fetchSeedColor(uri: Uri) {
        currentArtworkJob?.cancel()
        currentArtworkJob = viewModelScope.launch {
            delay(200)
            val extractedColor = DynamicThemeHelper.extractSeedColor(context, uri)
            if (activeArtworkUri == uri) {
                _playbackState.update { it.copy(seedColor = extractedColor) }
            }
        }
    }

    private fun fetchLyrics(songId: String) {
        viewModelScope.launch {
            val song = musicRepository.getSongById(songId) ?: return@launch

            if (_lyricsState.value !is LyricsUiState.None) return@launch

            _lyricsState.update { LyricsUiState.Loading }

            lyricsRepository.getLyrics(song)
                .onSuccess { lyrics ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        val isLocal = lyricsRepository.isLocalOrEmbeddedLyrics(song, lyrics.content)
                        _lyricsState.update {
                            LyricsUiState.Success(
                                lyrics = lyrics.content,
                                isSynced = lyrics.isSynced,
                                isLocal = isLocal
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        _lyricsState.update { LyricsUiState.Error(error.message ?: "Failed to load lyrics") }
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
        _progressState.update { it.copy(positionMs = player.currentPosition) }
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
        val player = controller ?: return
        val items = _queueState.value.items
        val currentIdx = _queueState.value.currentIndex
        val repeatMode = player.repeatMode

        if (items.isEmpty() || currentIdx < 0) {
            player.seekToNextMediaItem()
            return
        }

        if (repeatMode == Player.REPEAT_MODE_ONE) {
            if (items.size > 1 && currentIdx < items.size - 1) {
                onSkipToQueueItem(currentIdx + 1)
            } else {
                player.seekTo(0, 0L)
                player.play()
            }
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            if (items.size > 1) {
                val nextIdx = (currentIdx + 1) % items.size
                onSkipToQueueItem(nextIdx)
            } else {
                player.seekTo(0, 0L)
                player.play()
            }
        } else {
            if (currentIdx < items.size - 1) {
                onSkipToQueueItem(currentIdx + 1)
            } else {
                player.seekToNextMediaItem()
            }
        }
    }

    override fun onPrevious() {
        val player = controller ?: return
        val items = _queueState.value.items
        val currentIdx = _queueState.value.currentIndex
        val repeatMode = player.repeatMode

        if (items.isEmpty() || currentIdx < 0) {
            player.seekToPreviousMediaItem()
            return
        }

        if (repeatMode == Player.REPEAT_MODE_ONE) {
            if (items.size > 1 && currentIdx > 0) {
                onSkipToQueueItem(currentIdx - 1)
            } else {
                player.seekTo(0, 0L)
                player.play()
            }
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            if (items.size > 1) {
                val prevIdx = if (currentIdx > 0) currentIdx - 1 else items.size - 1
                onSkipToQueueItem(prevIdx)
            } else {
                player.seekTo(0, 0L)
                player.play()
            }
        } else {
            if (currentIdx > 0) {
                onSkipToQueueItem(currentIdx - 1)
            } else {
                player.seekToPreviousMediaItem()
            }
        }
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
            _lyricsState.update { LyricsUiState.Loading }

            lyricsRepository.searchLyrics(query, songId)
                .onSuccess { lyrics ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        val song = musicRepository.getSongById(songId)
                        val isLocal = if (song != null) {
                            lyricsRepository.isLocalOrEmbeddedLyrics(song, lyrics.content)
                        } else false
                        _lyricsState.update {
                            LyricsUiState.Success(
                                lyrics = lyrics.content,
                                isSynced = lyrics.isSynced,
                                isLocal = isLocal
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (controller?.currentMediaItem?.mediaId == songId) {
                        _lyricsState.update { LyricsUiState.Error(error.message ?: "Lyrics search failed") }
                    }
                }
        }
    }

    override fun onSaveLyrics() {
        val currentItem = controller?.currentMediaItem ?: return
        val songId = currentItem.mediaId
        val currentLyricsState = _lyricsState.value
        val lyricsContent = (currentLyricsState as? LyricsUiState.Success)?.lyrics ?: return

        viewModelScope.launch {
            val song = musicRepository.getSongById(songId)
            if (song != null) {
                val saved = lyricsRepository.saveLyrics(song, lyricsContent)
                if (saved) {
                    _lyricsState.update {
                        if (it is LyricsUiState.Success) it.copy(isLocal = true) else it
                    }
                }
            }
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
            val currentItems = _queueState.value.items
            if (index in currentItems.indices) {
                val newItems = currentItems.toMutableList().apply { removeAt(index) }
                val newWindowIndices = currentWindowIndices.toMutableList().apply { removeAt(index) }
                    .map { w -> if (w > targetWindowIndex) w - 1 else w }

                currentWindowIndices = newWindowIndices
                val uiCurrentIndex = player.currentMediaItemIndex.let { cur ->
                    val found = newWindowIndices.indexOf(cur)
                    if (found != -1) found else _queueState.value.currentIndex.coerceIn(0, (newItems.size - 1).coerceAtLeast(0))
                }

                _queueState.update {
                    QueueUiState(
                        items = newItems.toPersistentList(),
                        currentIndex = uiCurrentIndex
                    )
                }
            }
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
        val sortedUiIndices = indices.filter { it in _queueState.value.items.indices }.sortedDescending()
        if (sortedUiIndices.isEmpty()) return

        val newItems = _queueState.value.items.toMutableList()
        val newWindowIndices = currentWindowIndices.toMutableList()

        val targetWindowIndices = mutableListOf<Int>()
        for (uiIndex in sortedUiIndices) {
            val targetWindow = newWindowIndices.getOrNull(uiIndex) ?: uiIndex
            if (targetWindow in 0 until player.mediaItemCount) {
                targetWindowIndices.add(targetWindow)
                newItems.removeAt(uiIndex)
                newWindowIndices.removeAt(uiIndex)
                for (i in newWindowIndices.indices) {
                    if (newWindowIndices[i] > targetWindow) {
                        newWindowIndices[i] = newWindowIndices[i] - 1
                    }
                }
            }
        }

        currentWindowIndices = newWindowIndices
        val uiCurrentIndex = player.currentMediaItemIndex.let { cur ->
            val found = newWindowIndices.indexOf(cur)
            if (found != -1) found else _queueState.value.currentIndex.coerceIn(0, (newItems.size - 1).coerceAtLeast(0))
        }

        _queueState.update {
            QueueUiState(
                items = newItems.toPersistentList(),
                currentIndex = uiCurrentIndex
            )
        }

        for (wIndex in targetWindowIndices.sortedDescending()) {
            player.removeMediaItem(wIndex)
        }
    }

    override fun removeOthersFromQueue(indicesToKeep: Set<Int>) {
        val allIndices = _queueState.value.items.indices.toSet()
        val indicesToRemove = allIndices.filter { it !in indicesToKeep }.toSet()
        removeIndicesFromQueue(indicesToRemove)
    }

    override fun playNextInQueue(indices: Set<Int>) {
        val player = controller ?: return
        queueManager.executePlayNext(
            player = player,
            currentIdx = player.currentMediaItemIndex,
            indices = indices,
            windowIndices = currentWindowIndices
        )
        updateState()
    }

    override fun onUpdateVisualizerVisibility(isPlayerVisible: Boolean) {
        isVisualizerPlayerVisible = isPlayerVisible
        triggerVisualizerExtractionIfNeeded()
    }

    private fun triggerVisualizerExtractionIfNeeded() {
        val activeItem = controller?.currentMediaItem
        val activeId = activeItem?.mediaId?.takeIf { it.isNotEmpty() } ?: activeMediaId ?: ""
        val waveMode = _playbackState.value.playbackWave

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
        if (activeVisualizerSongId != activeId || activeVisualizerWave == waveMode) {
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
        playerListener?.let { controller?.removeListener(it) }
    }
}
