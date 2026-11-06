package io.github.vinaygaykar.mp1.ui.nowplaying

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import io.github.vinaygaykar.mp1.playback.LyricsControls
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueControls
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

private val samplePlaybackControls = object : PlaybackControls {
    override fun onPlayPause() {}
    override fun play() {}
    override fun pause() {}
    override fun onNext() {}
    override fun onPrevious() {}
    override fun onSeek(positionMs: Long) {}
    override fun onShuffleToggle() {}
    override fun onRepeatToggle() {}
}

private val sampleQueueControls = object : QueueControls {
    override fun addToQueue(mediaItem: MediaItem) {}
    override fun onSkipToQueueItem(index: Int) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {}
}

private val sampleLyricsControls = object : LyricsControls {
    override fun onLyricsSearch(query: String) {}
}

private val samplePlaybackState = PlaybackUiState(
    trackTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
    artist = "Consectetur Adipiscing, Sed Do Eiusmod, Tempor Incididunt",
    album = "Excepteur Sint Occaecat Cupidatat Non Proident Sunt In Culpa",
    albumId = "sample_album_lorem",
    isPlaying = true,
    hasNext = true,
    hasPrevious = true
)

private val sampleProgressState = PlaybackProgressState(
    positionMs = 125000L,
    durationMs = 243000L
)

private val sampleQueueState = QueueUiState()

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 1: Playing State
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split Light",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split Light",
    group = "1. Now Playing Screen - Playing",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun NowPlayingScreenPlayingPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(isPlaying = true),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 2: Paused State
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split Light",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split Light",
    group = "2. Now Playing Screen - Paused",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun NowPlayingScreenPausedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(isPlaying = false),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}
