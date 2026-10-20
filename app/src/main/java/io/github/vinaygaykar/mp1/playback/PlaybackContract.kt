package io.github.vinaygaykar.mp1.playback

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType

@Immutable
data class PlaybackUiState(
    val trackTitle: String = "",
    val artist: String = "",
    val album: String = "",
    val albumId: String? = null,
    val artworkUri: Uri? = null,
    val seedColor: Color? = null,
    val isPlaying: Boolean = false,
    val isShuffleOn: Boolean = false,
    val repeatMode: Int = 0, // Using Media3 RepeatMode values
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isScanInProgress: Boolean = false,
    val playbackWave: PlaybackWaveType = PlaybackWaveType.ENERGY,
)

@Immutable
data class PlaybackProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

interface PlaybackControls {
    fun onPlayPause()
    fun play()
    fun pause()
    fun stop() {}
    fun onNext()
    fun onPrevious()
    fun onSeek(positionMs: Long)
    fun onShuffleToggle()
    fun onRepeatToggle()
    fun onUpdateVisualizerVisibility(isPlayerVisible: Boolean) {}
}
