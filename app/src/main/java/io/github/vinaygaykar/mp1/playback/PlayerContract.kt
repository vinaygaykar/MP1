package io.github.vinaygaykar.mp1.playback

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

import io.github.vinaygaykar.mp1.domain.models.PlaybackWave

@Immutable
data class PlayerUiState(
    val trackTitle: String = "",
    val artist: String = "",
    val album: String = "",
    val albumId: String? = null,
    val artworkUri: Uri? = null,
    val seedColor: Color? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isShuffleOn: Boolean = false,
    val repeatMode: Int = 0, // Using Media3 RepeatMode values
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val lyrics: String? = null,
    val isLyricsSynced: Boolean = false,
    val isLyricsLocal: Boolean = false,
    val lyricsLoading: Boolean = false,
    val lyricsError: String? = null,
    val queue: ImmutableList<MediaItem> = persistentListOf(),
    val currentIndex: Int = -1,
    val isScanInProgress: Boolean = false,
    val playbackWave: PlaybackWave = PlaybackWave.SIMPLICITY
)

interface PlayerActions {
    fun onPlayPause()
    fun play()
    fun pause()
    fun stop() {}
    fun onNext()
    fun onPrevious()
    fun onSeek(positionMs: Long)
    fun onShuffleToggle()
    fun onRepeatToggle()
    fun addToQueue(mediaItem: MediaItem)
    fun onSkipToQueueItem(index: Int)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue()
    fun onLyricsSearch(query: String)
    fun onSaveLyrics() {}
    fun removeIndicesFromQueue(indices: Set<Int>) {}
    fun removeOthersFromQueue(indicesToKeep: Set<Int>) {}
    fun playNextInQueue(indices: Set<Int>) {}
    fun onUpdateVisualizerVisibility(isPlayerVisible: Boolean) {}
}
