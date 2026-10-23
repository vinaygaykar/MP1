package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun NowPlayingCollapseHandle(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    currentColorScheme: androidx.compose.material3.ColorScheme = MaterialTheme.colorScheme
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onCollapse() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(currentColorScheme.onPrimaryContainer.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun NowPlayingScreenBackground(
    state: NowPlayingScreenState,
    peekHeight: Dp,
    screenHeight: Dp,
    startPadding: Dp,
    endPadding: Dp,
    recordX: Dp,
    recordY: Dp,
    recordSize: Dp,
    cornerRadius: Dp,
    sheetShadowElevation: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceAlpha = 1f

    MaterialTheme(colorScheme = state.currentColorScheme) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(lerp(peekHeight, screenHeight, state.progress))
                .padding(start = startPadding, end = endPadding)
                .graphicsLayer {
                    scaleX = state.animatableBulge.value
                }
                .shadow(
                    elevation = sheetShadowElevation,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .drawBehind {
                    val localRecordX = recordX - startPadding
                    val center = Offset(
                        x = (localRecordX + recordSize / 2).toPx(),
                        y = (recordY + recordSize / 2).toPx()
                    )
                    val pColor =
                        (state.bgColorPair?.first ?: state.displayedColorState ?: state.targetBgColor).copy(alpha = surfaceAlpha)
                    val cColor =
                        (state.bgColorPair?.second ?: state.displayedColorState ?: state.targetBgColor).copy(alpha = surfaceAlpha)
                    val maxRadius = size.width.coerceAtLeast(size.height) * 1.5f

                    if (state.isReverseRipple) {
                        drawRect(cColor)
                        drawCircle(
                            color = pColor,
                            radius = maxRadius * (1f - state.rippleProgress.value),
                            center = center
                        )
                    } else {
                        drawRect(pColor)
                        drawCircle(
                            color = cColor,
                            radius = maxRadius * state.rippleProgress.value,
                            center = center
                        )
                    }
                },
            color = Color.Transparent,
            shape = RoundedCornerShape(cornerRadius),
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()

                if (state.progress > 0.8f) {
                    val controlsAlpha = ((state.progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    NowPlayingCollapseHandle(
                        onCollapse = { state.collapse() },
                        currentColorScheme = state.currentColorScheme,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .alpha(controlsAlpha)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Background - Light")
@Composable
fun NowPlayingScreenBackgroundLightPreview() {
    val samplePlaybackState = PlaybackUiState(
        trackTitle = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming"
    )
    val dummyControls = object : PlaybackControls {
        override fun onPlayPause() {}
        override fun play() {}
        override fun pause() {}
        override fun onNext() {}
        override fun onPrevious() {}
        override fun onSeek(positionMs: Long) {}
        override fun onShuffleToggle() {}
        override fun onRepeatToggle() {}
    }
    val state = rememberNowPlayingScreenState(
        playbackState = samplePlaybackState,
        queueState = io.github.vinaygaykar.mp1.playback.QueueUiState(),
        playbackControls = dummyControls,
        peekHeight = 72.dp,
        screenHeightPx = 2000f,
        totalBottomGapPx = 100f,
        initialExpanded = true,
        bulgeTrigger = 0L
    )

    MusicPlayerTheme(darkTheme = false) {
        NowPlayingScreenBackground(
            state = state,
            peekHeight = 72.dp,
            screenHeight = 600.dp,
            startPadding = 0.dp,
            endPadding = 0.dp,
            recordX = 100.dp,
            recordY = 200.dp,
            recordSize = 200.dp,
            cornerRadius = 16.dp,
            sheetShadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true, name = "Background - Dark")
@Composable
fun NowPlayingScreenBackgroundDarkPreview() {
    val samplePlaybackState = PlaybackUiState(
        trackTitle = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming"
    )
    val dummyControls = object : PlaybackControls {
        override fun onPlayPause() {}
        override fun play() {}
        override fun pause() {}
        override fun onNext() {}
        override fun onPrevious() {}
        override fun onSeek(positionMs: Long) {}
        override fun onShuffleToggle() {}
        override fun onRepeatToggle() {}
    }
    val state = rememberNowPlayingScreenState(
        playbackState = samplePlaybackState,
        queueState = io.github.vinaygaykar.mp1.playback.QueueUiState(),
        playbackControls = dummyControls,
        peekHeight = 72.dp,
        screenHeightPx = 2000f,
        totalBottomGapPx = 100f,
        initialExpanded = true,
        bulgeTrigger = 0L
    )

    MusicPlayerTheme(darkTheme = true) {
        NowPlayingScreenBackground(
            state = state,
            peekHeight = 72.dp,
            screenHeight = 600.dp,
            startPadding = 0.dp,
            endPadding = 0.dp,
            recordX = 100.dp,
            recordY = 200.dp,
            recordSize = 200.dp,
            cornerRadius = 16.dp,
            sheetShadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

