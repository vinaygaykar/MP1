package io.github.vinaygaykar.mp1.ui.nowplaying

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.HapticFeedbackConstantsCompat
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.ui.library.calculateAngle
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingVinylPager(
    state: NowPlayingScreenState,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    playbackControls: PlaybackControls,
    recordSize: Dp,
    artworkSize: Dp,
    startPadding: Dp,
    endPadding: Dp,
    recordX: Dp,
    recordY: Dp,
    screenWidth: Dp,
    overlayAlpha: Float,
    modifier: Modifier = Modifier,
    onMiniPlayerPositioned: ((Offset) -> Unit)? = null
) {
    val currentProgressState by rememberUpdatedState(progressState)
    val currentPlaybackState by rememberUpdatedState(playbackState)
    val currentPlaybackControls by rememberUpdatedState(playbackControls)

    val containerSize = recordSize * 1.15f
    val sizeDiff = (containerSize - recordSize) / 2

    val isExpanded = state.progress > 0.9f
    val containerWidth = screenWidth - startPadding - endPadding
    val defaultRecordX = (containerWidth - recordSize) / 2
    val targetRecordX = recordX - startPadding
    val offsetX = if (isExpanded) (targetRecordX - defaultRecordX) else (targetRecordX - sizeDiff)
    val offsetY = recordY - sizeDiff

    val maxSlidePx = with(state.density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .then(
                if (isExpanded) {
                    Modifier
                        .fillMaxWidth()
                        .height(containerSize)
                } else {
                    Modifier.size(containerSize)
                }
            )
            .offset {
                IntOffset(
                    x = with(state.density) { offsetX.toPx() }.roundToInt(),
                    y = with(state.density) { offsetY.toPx() }.roundToInt()
                )
            }
            .onGloballyPositioned { coords ->
                onMiniPlayerPositioned?.invoke(coords.positionInRoot())
            }
    ) {
        if (isExpanded) {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = state.pagerState,
                    userScrollEnabled = !state.showQueue && state.queueProgress == 0f && !state.showLyrics && !state.showArtistsDialog,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(state.boundaryNestedScrollConnection)
                        .pointerInput(artworkSize, recordSize) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                    val localCenter = Offset(size.width / 2f, size.height / 2f)
                                    val distFromCenter = (down.position - localCenter).getDistance()
                                    val artworkRadiusPx = with(state.density) { (artworkSize / 2).toPx() }
                                    val recordRadiusPx = with(state.density) { (recordSize / 2).toPx() }

                                    if (distFromCenter > artworkRadiusPx && state.queueProgress == 0f && !state.showQueue) {
                                        val isVinylRing = distFromCenter <= recordRadiusPx
                                        val pointerId = down.id
                                        var isDragging = false
                                        var wasPlayingOnDragStart = false
                                        var lastAngle = calculateAngle(down.position, localCenter)

                                        while (true) {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                            if (change.pressed) {
                                                val dragAmount = (change.position - down.position).getDistance()
                                                val touchSlop = viewConfiguration.touchSlop

                                                if (!isDragging) {
                                                    if (dragAmount > touchSlop) {
                                                        isDragging = true
                                                        change.consume()
                                                        if (isVinylRing) {
                                                            wasPlayingOnDragStart = currentPlaybackState.isPlaying
                                                            state.draggedPosition = currentProgressState.positionMs
                                                            state.hapticAccumulator = 0f
                                                            state.invalidHapticAccumulator = 0f
                                                            lastAngle = calculateAngle(change.position, localCenter)
                                                            if (wasPlayingOnDragStart) currentPlaybackControls.pause()
                                                        }
                                                    }
                                                } else {
                                                    change.consume()
                                                    if (isVinylRing) {
                                                        val currentCenter = Offset(size.width / 2f, size.height / 2f)
                                                        val currentAngle = calculateAngle(change.position, currentCenter)
                                                        var delta = currentAngle - lastAngle
                                                        if (delta > 180f) delta -= 360f
                                                        if (delta < -180f) delta += 360f

                                                        val positionDelta = (delta / 360f) * 60000f
                                                        val unconstrainedPosition = state.draggedPosition + positionDelta.toLong()
                                                        val minPos = 0L
                                                        val maxPos = currentProgressState.durationMs

                                                        if (unconstrainedPosition in minPos..maxPos) {
                                                            state.visualRotation += delta
                                                            state.draggedPosition = unconstrainedPosition
                                                            state.invalidHapticAccumulator = 0f

                                                            state.hapticAccumulator += abs(delta)
                                                            if (state.hapticAccumulator >= 10f) {
                                                                state.view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                                state.hapticAccumulator = 0f
                                                            }
                                                        } else {
                                                            val boundaryPos = if (unconstrainedPosition < minPos) minPos else maxPos
                                                            val posDeltaToBoundary = (boundaryPos - state.draggedPosition).toFloat()
                                                            val validFraction = if (positionDelta != 0f) {
                                                                (posDeltaToBoundary / positionDelta).coerceIn(0f, 1f)
                                                            } else {
                                                                0f
                                                            }

                                                            val validDelta = delta * validFraction
                                                            val invalidDelta = delta - validDelta

                                                            state.visualRotation += validDelta
                                                            state.draggedPosition = boundaryPos

                                                            val invalidDegrees = abs(invalidDelta)
                                                            if (state.invalidHapticAccumulator == 0f || state.invalidHapticAccumulator + invalidDegrees >= 12f) {
                                                                state.view.performHapticFeedback(HapticFeedbackConstantsCompat.REJECT)
                                                                state.invalidHapticAccumulator = if (state.invalidHapticAccumulator == 0f) 0.001f else ((state.invalidHapticAccumulator + invalidDegrees) % 12f)
                                                            } else {
                                                                state.invalidHapticAccumulator += invalidDegrees
                                                            }
                                                        }

                                                        lastAngle = currentAngle
                                                        state.isDraggingScrub = true
                                                    }
                                                }
                                            } else {
                                                // Released
                                                if (isDragging && isVinylRing) {
                                                    state.isDraggingScrub = false
                                                    currentPlaybackControls.onSeek(state.draggedPosition)
                                                    if (wasPlayingOnDragStart) currentPlaybackControls.play()
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) { page ->
                    val pageArtworkUri = when {
                        page == state.currentTrackPageIndex -> playbackState.artworkUri
                        state.hasPrevPage && page == state.currentTrackPageIndex - 1 -> state.prevArtworkUri
                        state.hasNextPage && page == state.currentTrackPageIndex + 1 -> state.nextArtworkUri
                        else -> null
                    }

                    val rawOffset = state.boundaryOffsetAnimatable.value
                    val slideTranslationX = rawOffset.coerceIn(-maxSlidePx, maxSlidePx)
                    val excessPx = (abs(rawOffset) - maxSlidePx).coerceAtLeast(0f)
                    val stretchScaleX = 1f + (excessPx / 3200f).coerceAtMost(0.05f)

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        RecordSurface(
                            artworkUri = pageArtworkUri,
                            recordSize = recordSize,
                            artworkSize = artworkSize,
                            rotation = if (page == state.currentTrackPageIndex) state.visualRotation else 0f,
                            overlayAlpha = if (page == state.currentTrackPageIndex) overlayAlpha else 0f,
                            modifier = Modifier
                                .size(containerSize)
                                .pointerInput(artworkSize) {
                                    detectTapGestures(
                                        onPress = { pressOffset ->
                                            val localCenter = Offset(size.width / 2f, size.height / 2f)
                                            val distFromCenter = (pressOffset - localCenter).getDistance()
                                            val artworkRadiusPx = with(state.density) { (artworkSize / 2).toPx() }
                                            if (distFromCenter <= artworkRadiusPx) {
                                                state.view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                state.scale.animateTo(0.96f, animationSpec = tween(100))
                                                try {
                                                    awaitRelease()
                                                    currentPlaybackControls.onPlayPause()
                                                } finally {
                                                    state.scale.animateTo(
                                                        1.0f,
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    if (page == state.currentTrackPageIndex && rawOffset != 0f) {
                                        translationX = slideTranslationX
                                        scaleX = stretchScaleX
                                    }
                                }
                                .scale(state.scale.value * 0.85f),
                            isScrubbing = page == state.currentTrackPageIndex && state.isDraggingScrub,
                            isDraggingArtwork = page == state.currentTrackPageIndex && state.isDraggingArtwork
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                RecordSurface(
                    artworkUri = playbackState.artworkUri,
                    recordSize = recordSize,
                    artworkSize = artworkSize,
                    rotation = state.visualRotation,
                    overlayAlpha = 0f,
                    modifier = Modifier
                        .size(containerSize)
                        .pointerInput(artworkSize) {
                            detectTapGestures(
                                onPress = { pressOffset ->
                                    val localCenter = Offset(size.width / 2f, size.height / 2f)
                                    val distFromCenter = (pressOffset - localCenter).getDistance()
                                    val artworkRadiusPx = with(state.density) { (artworkSize / 2).toPx() }
                                    if (distFromCenter <= artworkRadiusPx) {
                                        currentPlaybackControls.onPlayPause()
                                    }
                                }
                            )
                        }
                        .scale(state.scale.value * 0.85f),
                    isScrubbing = false,
                    isDraggingArtwork = false
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Vinyl Pager - Light")
@Composable
fun NowPlayingVinylPagerLightPreview() {
    val samplePlaybackState = PlaybackUiState(
        trackTitle = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        isPlaying = true
    )
    val sampleProgressState = PlaybackProgressState(120000L, 240000L)
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
        Box(
            modifier = Modifier
                .size(360.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            NowPlayingVinylPager(
                state = state,
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                playbackControls = dummyControls,
                recordSize = 260.dp,
                artworkSize = 160.dp,
                startPadding = 0.dp,
                endPadding = 0.dp,
                recordX = 50.dp,
                recordY = 50.dp,
                screenWidth = 360.dp,
                overlayAlpha = 0f
            )
        }
    }
}

@Preview(showBackground = true, name = "Vinyl Pager - Dark")
@Composable
fun NowPlayingVinylPagerDarkPreview() {
    val samplePlaybackState = PlaybackUiState(
        trackTitle = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        isPlaying = true
    )
    val sampleProgressState = PlaybackProgressState(120000L, 240000L)
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
        Box(
            modifier = Modifier
                .size(360.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            NowPlayingVinylPager(
                state = state,
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                playbackControls = dummyControls,
                recordSize = 260.dp,
                artworkSize = 160.dp,
                startPadding = 0.dp,
                endPadding = 0.dp,
                recordX = 50.dp,
                recordY = 50.dp,
                screenWidth = 360.dp,
                overlayAlpha = 0f
            )
        }
    }
}

