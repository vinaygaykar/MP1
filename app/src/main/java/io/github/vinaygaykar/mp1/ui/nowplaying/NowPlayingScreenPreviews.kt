package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType
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
    trackTitle = "Midnight City",
    artist = "M83",
    album = "Hurry Up, We're Dreaming",
    albumId = "sample_album_1",
    isPlaying = true,
    hasNext = true,
    hasPrevious = true
)

private val sampleProgressState = PlaybackProgressState(
    positionMs = 125000L,
    durationMs = 243000L
)

private val sampleQueueState = QueueUiState()

@Preview(showBackground = true, name = "Portrait Phone Expanded", device = "spec:width=411dp,height=891dp,orientation=portrait")
@Composable
fun NowPlayingScreenPortraitExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState,
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

@Preview(showBackground = true, name = "Landscape Phone Expanded", device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun NowPlayingScreenLandscapeExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState,
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

@Preview(showBackground = true, name = "Foldable / Tablet Expanded", device = "spec:width=800dp,height=800dp")
@Composable
fun NowPlayingScreenFoldableExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState,
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

@Preview(showBackground = true, name = "Playing State", device = "id:pixel_7")
@Composable
fun NowPlayingScreenPlayingPreview() {
    MusicPlayerTheme {
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

@Preview(showBackground = true, name = "Paused State", device = "id:pixel_7")
@Composable
fun NowPlayingScreenPausedStatePreview() {
    MusicPlayerTheme {
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

@Preview(showBackground = true, name = "Short / Medium Title", device = "id:pixel_7")
@Composable
fun NowPlayingScreenShortTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(trackTitle = "Midnight City"),
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

@Preview(showBackground = true, name = "Long Multi-line Title", device = "id:pixel_7")
@Composable
fun NowPlayingScreenLongTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(
                    trackTitle = "Supercalifragilisticexpialidocious Long Track Title That Needs Auto Resizing To Fit On Two Lines"
                ),
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

@Preview(showBackground = true, name = "Multiple Artists", device = "id:pixel_7")
@Composable
fun NowPlayingScreenMultipleArtistsPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(
                    artist = "M83, Anthony Gonzalez, Morgan Kibby"
                ),
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

@Preview(showBackground = true, name = "Energy Waveform", device = "id:pixel_7")
@Composable
fun NowPlayingScreenEnergyWaveformPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState.copy(playbackWave = PlaybackWaveType.ENERGY),
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

@Preview(showBackground = true, name = "Light Theme", device = "id:pixel_7")
@Composable
fun NowPlayingScreenLightThemePreview() {
    MusicPlayerTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState,
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

@Preview(showBackground = true, name = "Dark Theme", device = "id:pixel_7")
@Composable
fun NowPlayingScreenDarkThemePreview() {
    MusicPlayerTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NowPlayingScreen(
                playbackState = samplePlaybackState,
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
