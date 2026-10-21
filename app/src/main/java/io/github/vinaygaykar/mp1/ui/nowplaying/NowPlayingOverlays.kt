package io.github.vinaygaykar.mp1.ui.nowplaying

import android.content.Intent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vinaygaykar.mp1.playback.LyricsControls
import io.github.vinaygaykar.mp1.playback.LyricsUiState
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueControls
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.toArtistId
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingOverlays(
    state: NowPlayingScreenState,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    queueState: QueueUiState,
    lyricsState: LyricsUiState,
    playbackControls: PlaybackControls,
    queueControls: QueueControls,
    lyricsControls: LyricsControls,
    view: View
) {
    val context = LocalContext.current

    // Lyrics Overlay
    AnimatedVisibility(
        visible = state.showLyrics,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        LyricsScreen(
            lyricsState = lyricsState,
            playbackState = playbackState,
            progressState = progressState,
            actions = lyricsControls,
            onSeek = { playbackControls.onSeek(it) }
        )
    }

    // Queue Overlay Sheet
    if (state.queueProgress > 0f || state.showQueue) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = state.queueProgress }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = state.queueProgress > 0f
                ) {
                    state.collapseQueue()
                }
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, state.queueOffset.roundToInt()) }
                .anchoredDraggable(state.queueAnchoredDraggableState, Orientation.Vertical),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 8.dp
        ) {
            QueueBottomSheetContent(
                queueState = queueState,
                actions = queueControls,
                isShuffleOn = playbackState.isShuffleOn,
                onShuffleToggle = { playbackControls.onShuffleToggle() },
                onDismiss = {
                    state.collapseQueue()
                }
            )
        }
    }

    // Song Artists Dialog
    if (state.showArtistsDialog) {
        val artistNames = remember(playbackState.artist, playbackState.trackTitle) {
            collectArtistNames(playbackState.artist, playbackState.trackTitle)
        }
        SongArtistsDialog(
            trackTitle = playbackState.trackTitle,
            artistNames = artistNames,
            onDismissRequest = { state.showArtistsDialog = false },
            onArtistClick = { selectedArtistName ->
                state.showArtistsDialog = false
                val intent = Intent(context, ArtistDetailActivity::class.java).apply {
                    putExtra(
                        ArtistDetailActivity.EXTRA_ARTIST_ID,
                        selectedArtistName.toArtistId()
                    )
                }
                launchActivityWithScaleUp(context, intent, view)
            }
        )
    }
}
