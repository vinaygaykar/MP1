package io.github.vinaygaykar.mp1.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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
import io.github.vinaygaykar.mp1.domain.repository.LyricsRepository
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsRepository: LyricsRepository,
    private val musicRepository: MusicRepository,
    private val scannerRepository: ScannerRepository
) : ViewModel(), PlayerActions {

    private var controller: MediaController? = null
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var currentArtworkJob: Job? = null
    private var queueUpdateJob: Job? = null
    private var activeArtworkUri: Uri? = null
    private var activeMediaId: String? = null

    init {
        viewModelScope.launch {
            scannerRepository.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanInProgress = isScanning) }
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
            _uiState.update { PlayerUiState(isScanInProgress = it.isScanInProgress) }
            return
        }

        val mediaId = currentItem.mediaId
        val extras = currentItem.mediaMetadata.extras
        val localLyrics = extras?.getString("lyrics")
        val localIsSynced = extras?.getBoolean("is_lyrics_synced") ?: false
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
        val lyricsLoadingToUse: Boolean
        val lyricsErrorToUse: String?

        if (isNewMediaItem) {
            lyricsToUse = localLyrics
            isSyncedToUse = localIsSynced
            lyricsLoadingToUse = false
            lyricsErrorToUse = null
        } else {
            lyricsToUse = _uiState.value.lyrics ?: localLyrics
            isSyncedToUse =
                if (_uiState.value.lyrics != null) _uiState.value.isLyricsSynced else localIsSynced
            lyricsLoadingToUse =
                if (_uiState.value.lyrics != null) false else _uiState.value.lyricsLoading
            lyricsErrorToUse =
                if (_uiState.value.lyrics != null) null else _uiState.value.lyricsError
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
                lyricsLoading = lyricsLoadingToUse,
                lyricsError = lyricsErrorToUse,
                queue = _uiState.value.queue,
                currentIndex = player.currentMediaItemIndex
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

        if (lyricsToUse == null && !lyricsLoadingToUse && lyricsErrorToUse == null && localLyrics == null) {
            fetchLyrics(mediaId)
        }
    }

    private fun updateQueueFromTimeline(timeline: Timeline) {
        if (timeline.isEmpty) {
            queueUpdateJob?.cancel()
            _uiState.update { it.copy(queue = persistentListOf()) }
            return
        }
        queueUpdateJob?.cancel()
        queueUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce rapid consecutive timeline changes during chunk loading
            delay(200)
            val count = timeline.windowCount
            val window = Timeline.Window()
            val newQueue = ArrayList<MediaItem>(count)
            for (i in 0 until count) {
                timeline.getWindow(i, window)
                newQueue.add(window.mediaItem)
            }
            _uiState.update { it.copy(queue = newQueue.toPersistentList()) }
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
                        _uiState.update {
                            it.copy(
                                lyrics = lyrics.content,
                                isLyricsSynced = lyrics.isSynced,
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
                        _uiState.update {
                            it.copy(
                                lyrics = lyrics.content,
                                isLyricsSynced = lyrics.isSynced,
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

    override fun addToQueue(mediaItem: MediaItem) {
        controller?.addMediaItem(mediaItem)
    }

    override fun onSkipToQueueItem(index: Int) {
        controller?.let {
            it.seekTo(index, 0L)
            it.play()
        }
    }

    override fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            _uiState.update { currentState ->
                val mutableQueue = currentState.queue.toMutableList()
                if (index in mutableQueue.indices) {
                    mutableQueue.removeAt(index)
                    var newCurrentIndex = currentState.currentIndex
                    if (index < currentState.currentIndex) {
                        newCurrentIndex = (newCurrentIndex - 1).coerceAtLeast(0)
                    } else if (index == currentState.currentIndex) {
                        newCurrentIndex =
                            newCurrentIndex.coerceAtMost((mutableQueue.size - 1).coerceAtLeast(0))
                    }
                    currentState.copy(
                        queue = mutableQueue.toPersistentList(),
                        currentIndex = newCurrentIndex,
                        hasNext = player.hasNextMediaItem(),
                        hasPrevious = player.hasPreviousMediaItem()
                    )
                } else currentState
            }
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller
        if (player != null && fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount && fromIndex != toIndex) {
            if (player.shuffleModeEnabled) {
                player.shuffleModeEnabled = false
            }
            player.moveMediaItem(fromIndex, toIndex)
        }

        _uiState.update { currentState ->
            val mutableQueue = currentState.queue.toMutableList()
            if (fromIndex in mutableQueue.indices && toIndex in mutableQueue.indices && fromIndex != toIndex) {
                val movedItem = mutableQueue.removeAt(fromIndex)
                mutableQueue.add(toIndex, movedItem)

                var newCurrentIndex = currentState.currentIndex
                if (currentState.currentIndex == fromIndex) {
                    newCurrentIndex = toIndex
                } else if (fromIndex < currentState.currentIndex && toIndex >= currentState.currentIndex) {
                    newCurrentIndex -= 1
                } else if (fromIndex > currentState.currentIndex && toIndex <= currentState.currentIndex) {
                    newCurrentIndex += 1
                }

                currentState.copy(
                    queue = mutableQueue.toPersistentList(),
                    currentIndex = newCurrentIndex,
                    isShuffleOn = player?.shuffleModeEnabled ?: false,
                    hasNext = player?.hasNextMediaItem()
                        ?: (newCurrentIndex < mutableQueue.size - 1),
                    hasPrevious = player?.hasPreviousMediaItem() ?: (newCurrentIndex > 0)
                )
            } else currentState
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
            val currentItem = player.currentMediaItem
            val newQueue = if (currentItem != null) persistentListOf(currentItem) else persistentListOf()
            _uiState.update { currentState ->
                currentState.copy(
                    queue = newQueue,
                    currentIndex = 0,
                    hasNext = false,
                    hasPrevious = false
                )
            }
        } else {
            player.clearMediaItems()
            _uiState.update { currentState ->
                currentState.copy(
                    queue = persistentListOf(),
                    currentIndex = -1,
                    hasNext = false,
                    hasPrevious = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
