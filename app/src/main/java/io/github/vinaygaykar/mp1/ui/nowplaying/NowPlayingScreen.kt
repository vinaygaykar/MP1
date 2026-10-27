package io.github.vinaygaykar.mp1.ui.nowplaying

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import io.github.vinaygaykar.mp1.domain.models.VisualizerState
import io.github.vinaygaykar.mp1.playback.LyricsControls
import io.github.vinaygaykar.mp1.playback.LyricsUiState
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueControls
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.library.DragValue
import io.github.vinaygaykar.mp1.utils.AudioEnergyExtractor
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("DiscouragedApi", "InternalInsetResource")
@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    queueState: QueueUiState,
    lyricsState: LyricsUiState = LyricsUiState.None,
    playbackControls: PlaybackControls,
    queueControls: QueueControls,
    lyricsControls: LyricsControls,
    onProgressUpdate: (Float) -> Unit,
    visualizerState: VisualizerState = VisualizerState.None,
    peekHeight: Dp = 72.dp,
    bottomOffset: Dp = 0.dp,
    initialExpanded: Boolean = false,
    bulgeTrigger: Long = 0L,
    isCollapsedOnScroll: Boolean = false,
    onMiniPlayerPositioned: ((Offset) -> Unit)? = null
) {
    if (playbackState.trackTitle.isEmpty()) return

    val density = LocalDensity.current
    val context = LocalContext.current
    val view = LocalView.current

    val currentPlaybackControls by rememberUpdatedState(playbackControls)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight
        val screenWidth = this.maxWidth

        val peekHeightPx = with(density) { peekHeight.toPx() }
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val bottomOffsetPx = with(density) { bottomOffset.toPx() }
        val floatOffsetPx = with(density) { 16.dp.toPx() }
        val navBarResourceHeight = remember(context) {
            val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        }
        val composeNavBarBottom = WindowInsets.navigationBars.getBottom(density)
        val navBarBottomPx = maxOf(composeNavBarBottom, navBarResourceHeight).toFloat()
        val totalBottomGapPx = bottomOffsetPx + floatOffsetPx + navBarBottomPx

        val state = rememberNowPlayingScreenState(
            playbackState = playbackState,
            queueState = queueState,
            playbackControls = playbackControls,
            peekHeight = peekHeight,
            screenHeightPx = screenHeightPx,
            totalBottomGapPx = totalBottomGapPx,
            initialExpanded = initialExpanded,
            bulgeTrigger = bulgeTrigger,
            isCollapsedOnScroll = isCollapsedOnScroll
        )

        BackHandler(
            enabled = state.anchoredDraggableState.currentValue != DragValue.COLLAPSED ||
                    state.queueProgress > 0f || state.showQueue
        ) {
            if (state.showArtistsDialog) {
                state.showArtistsDialog = false
            } else if (state.showLyrics) {
                state.showLyrics = false
            } else if (state.queueProgress > 0f || state.showQueue) {
                state.collapseQueue()
            } else {
                state.collapse()
            }
        }

        LaunchedEffect(state.progress) {
            onProgressUpdate(state.progress)
        }

        val isPlayerVisible = state.progress > 0.8f && state.isScreenActive
        val activeSongId = remember(queueState.items, queueState.currentIndex) {
            if (queueState.currentIndex in queueState.items.indices) {
                queueState.items[queueState.currentIndex].mediaId
            } else {
                "${queueState.currentIndex}_${playbackState.trackTitle}"
            }
        }

        LaunchedEffect(activeSongId, isPlayerVisible, playbackState.playbackWave) {
            currentPlaybackControls.onUpdateVisualizerVisibility(isPlayerVisible)
        }

        val horizontalPadding = lerp(20.dp, 0.dp, state.progress)

        val rawStartPadding = lerp(screenWidth - 92.dp, 20.dp, state.effectiveExpandProgress)
        val rawEndPadding = 20.dp

        val startPadding = lerp(rawStartPadding, 0.dp, state.progress)
        val endPadding = lerp(rawEndPadding, 0.dp, state.progress)

        val isLandscape = screenWidth > screenHeight

        val cornerRadius = lerp(50.dp, 0.dp, state.progress)
        val sheetShadowElevation = lerp(8.dp, 0.dp, state.progress)

        val targetRecordSize = if (isLandscape) {
            min(screenHeight * 0.72f, screenWidth * 0.42f)
        } else {
            screenWidth * 0.85f
        }
        val recordSize = lerp(56.dp, targetRecordSize, state.progress)
        val artworkSize = lerp(56.dp, min(210.dp, recordSize * 0.6f), state.progress)

        val targetRecordX = if (isLandscape) {
            screenWidth * 0.50f + (screenWidth * 0.50f - targetRecordSize) / 2
        } else {
            (screenWidth - targetRecordSize) / 2 - horizontalPadding
        }
        val recordX = lerp(screenWidth - recordSize - 8.dp - horizontalPadding, targetRecordX, state.progress)

        val targetRecordY = if (isLandscape) {
            (screenHeight - targetRecordSize) / 2
        } else {
            screenHeight * 0.44f
        }
        val recordY = lerp(8.dp, targetRecordY, state.progress)

        val overlayAlpha by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400),
            label = "overlay_alpha"
        )

        val scrubFactor by animateFloatAsState(
            targetValue = if (state.isDraggingScrub) 1f else 0f,
            animationSpec = tween(400),
            label = "scrub_factor"
        )

        val rowWidthFraction by animateFloatAsState(
            targetValue = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "row_width_fraction"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(peekHeight, screenHeight, state.progress))
                .offset { IntOffset(0, state.dragOffset.roundToInt()) }
                .anchoredDraggable(state.anchoredDraggableState, Orientation.Vertical)
        ) {
            NowPlayingScreenBackground(
                state = state,
                peekHeight = peekHeight,
                screenHeight = screenHeight,
                startPadding = startPadding,
                endPadding = endPadding,
                recordX = recordX,
                recordY = recordY,
                recordSize = recordSize,
                cornerRadius = cornerRadius,
                sheetShadowElevation = sheetShadowElevation
            ) {
                val controlsAlpha = ((state.progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                val expandedAlpha = ((state.progress - 0.05f) / 0.15f).coerceIn(0f, 1f)

                if (isLandscape) {
                    NowPlayingLandscapeContent(
                        state = state,
                        playbackState = playbackState,
                        progressState = progressState,
                        queueState = queueState,
                        lyricsState = lyricsState,
                        playbackControls = playbackControls,
                        queueControls = queueControls,
                        lyricsControls = lyricsControls,
                        visualizerState = visualizerState,
                        recordSize = recordSize,
                        artworkSize = artworkSize,
                        startPadding = startPadding,
                        endPadding = endPadding,
                        recordX = recordX,
                        recordY = recordY,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        peekHeight = peekHeight,
                        overlayAlpha = overlayAlpha,
                        scrubFactor = scrubFactor,
                        controlsAlpha = controlsAlpha,
                        expandedAlpha = expandedAlpha,
                        onMiniPlayerPositioned = onMiniPlayerPositioned,
                        view = view
                    )
                } else {
                    NowPlayingPortraitContent(
                        state = state,
                        playbackState = playbackState,
                        progressState = progressState,
                        queueState = queueState,
                        lyricsState = lyricsState,
                        playbackControls = playbackControls,
                        queueControls = queueControls,
                        lyricsControls = lyricsControls,
                        visualizerState = visualizerState,
                        recordSize = recordSize,
                        artworkSize = artworkSize,
                        startPadding = startPadding,
                        endPadding = endPadding,
                        recordX = recordX,
                        recordY = recordY,
                        screenWidth = screenWidth,
                        peekHeight = peekHeight,
                        rowWidthFraction = rowWidthFraction,
                        overlayAlpha = overlayAlpha,
                        scrubFactor = scrubFactor,
                        controlsAlpha = controlsAlpha,
                        expandedAlpha = expandedAlpha,
                        onMiniPlayerPositioned = onMiniPlayerPositioned,
                        view = view
                    )
                }
            }
        }
    }
}

// TODO: move somewhere else
private fun calculateEnergy(
    visualizerState: VisualizerState,
    displayPosition: Long
): Pair<Float?, Float> {
    return if (visualizerState is VisualizerState.Energy) {
        val curve = visualizerState.curve
        val bucketIndex = (displayPosition / AudioEnergyExtractor.ENERGY_INTERVAL_MS).toInt()
        if (bucketIndex in curve.indices) {
            val curr = curve[bucketIndex]
            val prev = if (bucketIndex > 0) curve[bucketIndex - 1] else curr
            val delta = abs(curr - prev)
            (curr as Float?) to delta
        } else {
            null to 0f
        }
    } else {
        null to 0f
    }
}

@Composable
private fun NowPlayingPortraitContent(
    state: NowPlayingScreenState,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    queueState: QueueUiState,
    lyricsState: LyricsUiState,
    playbackControls: PlaybackControls,
    queueControls: QueueControls,
    lyricsControls: LyricsControls,
    visualizerState: VisualizerState,
    recordSize: Dp,
    artworkSize: Dp,
    startPadding: Dp,
    endPadding: Dp,
    recordX: Dp,
    recordY: Dp,
    screenWidth: Dp,
    peekHeight: Dp,
    rowWidthFraction: Float,
    overlayAlpha: Float,
    scrubFactor: Float,
    controlsAlpha: Float,
    expandedAlpha: Float,
    onMiniPlayerPositioned: ((Offset) -> Unit)?,
    view: android.view.View
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(state.progress, state.showQueue, state.showLyrics, state.showArtistsDialog, state.totalRange) {
                if (state.progress > 0.8f && !state.showQueue && !state.showLyrics && !state.showArtistsDialog) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            val currentPlayerOffset = try {
                                state.anchoredDraggableState.requireOffset()
                            } catch (_: Exception) { 0f }
                            if (dragAmount > 0f) {
                                if (currentPlayerOffset < state.totalRange) {
                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtMost(state.totalRange)
                                    state.anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                    change.consume()
                                }
                            } else if (dragAmount < 0f) {
                                if (currentPlayerOffset > 0f) {
                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtLeast(0f)
                                    state.anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                    change.consume()
                                }
                            }
                        },
                        onDragEnd = {
                            val currentPlayerOffset = try { state.anchoredDraggableState.requireOffset() } catch (_: Exception) { 0f }
                            if (currentPlayerOffset > 0f) {
                                if (currentPlayerOffset > state.totalRange * 0.4f) state.collapse() else state.expand()
                            }
                        },
                        onDragCancel = {
                            val currentPlayerOffset = try { state.anchoredDraggableState.requireOffset() } catch (_: Exception) { 0f }
                            if (currentPlayerOffset > 0f) {
                                if (currentPlayerOffset > state.totalRange * 0.4f) state.collapse() else state.expand()
                            }
                        }
                    )
                }
            }
    ) {
        // Track Content
        if (state.progress > 0.05f) {
            NowPlayingTrackInfo(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp)
                    .padding(horizontal = 24.dp)
                    .alpha(expandedAlpha),
                playbackState = playbackState,
                queueState = queueState,
                pagerState = state.pagerState
            ) {
                state.showArtistsDialog = true
            }
        }

        // Collapsed View Content
        if (state.progress < 0.2f) {
            val miniPlayerAlpha = ((0.2f - state.progress) / 0.2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(peekHeight)
                    .alpha(miniPlayerAlpha)
                    .clickable { state.expand() }
            ) {
                NowPlayingCollapsedView(
                    playbackState = playbackState,
                    progressState = progressState,
                    queueState = queueState,
                    actions = playbackControls,
                    showRecord = false,
                    rotation = { state.visualRotation },
                    textAlpha = { ((state.effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f) },
                    onClick = { state.expand() }
                )
            }
        }

        // Action Buttons
        NowPlayingActionButtons(
            state = state,
            playbackState = playbackState,
            playbackControls = playbackControls,
            isLandscape = false
        )

        // Waveform / Timestamp Bar
        if (state.progress > 0.8f) {
            val displayPosition = if (state.isDraggingScrub) state.draggedPosition else progressState.positionMs
            val (currentEnergy, currentEnergyDelta) = calculateEnergy(visualizerState, displayPosition)
            PlayerTimestampBar(
                displayPositionMs = displayPosition,
                durationMs = progressState.durationMs,
                isPlaying = playbackState.isPlaying,
                scrubFactor = scrubFactor,
                rowWidthFraction = rowWidthFraction,
                recordY = recordY,
                progress = controlsAlpha,
                energy = currentEnergy,
                energyDelta = currentEnergyDelta,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Vinyl Record Pager
        NowPlayingVinylPager(
            state = state,
            playbackState = playbackState,
            progressState = progressState,
            playbackControls = playbackControls,
            recordSize = recordSize,
            artworkSize = artworkSize,
            startPadding = startPadding,
            endPadding = endPadding,
            recordX = recordX,
            recordY = recordY,
            screenWidth = screenWidth,
            overlayAlpha = overlayAlpha,
            onMiniPlayerPositioned = onMiniPlayerPositioned
        )

        // Overlays
        NowPlayingOverlays(
            state = state,
            playbackState = playbackState,
            progressState = progressState,
            queueState = queueState,
            lyricsState = lyricsState,
            playbackControls = playbackControls,
            queueControls = queueControls,
            lyricsControls = lyricsControls,
            view = view
        )
    }
}

@Composable
private fun NowPlayingLandscapeContent(
    state: NowPlayingScreenState,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    queueState: QueueUiState,
    lyricsState: LyricsUiState,
    playbackControls: PlaybackControls,
    queueControls: QueueControls,
    lyricsControls: LyricsControls,
    visualizerState: VisualizerState,
    recordSize: Dp,
    artworkSize: Dp,
    startPadding: Dp,
    endPadding: Dp,
    recordX: Dp,
    recordY: Dp,
    screenWidth: Dp,
    screenHeight: Dp,
    peekHeight: Dp,
    overlayAlpha: Float,
    scrubFactor: Float,
    controlsAlpha: Float,
    expandedAlpha: Float,
    onMiniPlayerPositioned: ((Offset) -> Unit)?,
    view: android.view.View
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(state.progress, state.showQueue, state.showLyrics, state.showArtistsDialog, state.totalRange) {
                if (state.progress > 0.8f && !state.showQueue && !state.showLyrics && !state.showArtistsDialog) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            val currentPlayerOffset = try {
                                state.anchoredDraggableState.requireOffset()
                            } catch (_: Exception) { 0f }
                            if (dragAmount > 0f) {
                                if (currentPlayerOffset < state.totalRange) {
                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtMost(state.totalRange)
                                    state.anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                    change.consume()
                                }
                            } else if (dragAmount < 0f) {
                                if (currentPlayerOffset > 0f) {
                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtLeast(0f)
                                    state.anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                    change.consume()
                                }
                            }
                        },
                        onDragEnd = {
                            val currentPlayerOffset = try { state.anchoredDraggableState.requireOffset() } catch (_: Exception) { 0f }
                            if (currentPlayerOffset > 0f) {
                                if (currentPlayerOffset > state.totalRange * 0.4f) state.collapse() else state.expand()
                            }
                        },
                        onDragCancel = {
                            val currentPlayerOffset = try { state.anchoredDraggableState.requireOffset() } catch (_: Exception) { 0f }
                            if (currentPlayerOffset > 0f) {
                                if (currentPlayerOffset > state.totalRange * 0.4f) state.collapse() else state.expand()
                            }
                        }
                    )
                }
            }
    ) {
        // Left 50% Column (Track Content & Waveform directly below with 12dp margin)
        if (state.progress > 0.05f) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(screenWidth * 0.50f)
                    .padding(top = 48.dp)
                    .alpha(expandedAlpha)
            ) {
                NowPlayingTrackInfo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    playbackState = playbackState,
                    queueState = queueState,
                    pagerState = state.pagerState,
                    titleMaxLines = 1
                ) {
                    state.showArtistsDialog = true
                }

                if (state.progress > 0.8f) {
                    val displayPosition = if (state.isDraggingScrub) state.draggedPosition else progressState.positionMs
                    val (currentEnergy, currentEnergyDelta) = calculateEnergy(visualizerState, displayPosition)
                    val landscapeWaveCycles = remember(currentEnergy) {
                        val energyVal = (currentEnergy ?: 0.2f).coerceIn(0f, 1f)
                        val energyForCycles = ((energyVal - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        5.0f + (energyForCycles * 3.0f)
                    }
                    PlayerTimestampBar(
                        displayPositionMs = displayPosition,
                        durationMs = progressState.durationMs,
                        isPlaying = playbackState.isPlaying,
                        scrubFactor = scrubFactor,
                        rowWidthFraction = 1f,
                        recordY = null,
                        progress = controlsAlpha,
                        energy = currentEnergy,
                        energyDelta = currentEnergyDelta,
                        waveCycles = landscapeWaveCycles,
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // Collapsed View Content
        if (state.progress < 0.2f) {
            val miniPlayerAlpha = ((0.2f - state.progress) / 0.2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(peekHeight)
                    .alpha(miniPlayerAlpha)
                    .clickable { state.expand() }
            ) {
                NowPlayingCollapsedView(
                    playbackState = playbackState,
                    progressState = progressState,
                    queueState = queueState,
                    actions = playbackControls,
                    showRecord = false,
                    rotation = { state.visualRotation },
                    textAlpha = { ((state.effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f) },
                    onClick = { state.expand() }
                )
            }
        }

        // Action Buttons (Left 50% Pane, bottom left)
        NowPlayingActionButtons(
            state = state,
            playbackState = playbackState,
            playbackControls = playbackControls,
            isLandscape = true
        )

        // Vinyl Record Pager (Floating in Right 50% Pane)
        NowPlayingVinylPager(
            state = state,
            playbackState = playbackState,
            progressState = progressState,
            playbackControls = playbackControls,
            recordSize = recordSize,
            artworkSize = artworkSize,
            startPadding = startPadding,
            endPadding = endPadding,
            recordX = recordX,
            recordY = recordY,
            screenWidth = screenWidth,
            overlayAlpha = overlayAlpha,
            onMiniPlayerPositioned = onMiniPlayerPositioned
        )

        // Overlays
        NowPlayingOverlays(
            state = state,
            playbackState = playbackState,
            progressState = progressState,
            queueState = queueState,
            lyricsState = lyricsState,
            playbackControls = playbackControls,
            queueControls = queueControls,
            lyricsControls = lyricsControls,
            view = view
        )
    }
}
