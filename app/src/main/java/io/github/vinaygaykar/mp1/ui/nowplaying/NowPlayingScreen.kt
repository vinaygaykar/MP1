package io.github.vinaygaykar.mp1.ui.nowplaying

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
    targetState: DragValue? = null,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onMiniPlayerPositioned: ((Offset) -> Unit)? = null
) {
    if (playbackState.trackTitle.isEmpty()) return

    val density = LocalDensity.current
    val context = LocalContext.current

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

        LaunchedEffect(targetState) {
            targetState?.let { state.moveToState(it) }
        }

        val isBackHandlerEnabled = state.showArtistsDialog ||
                state.showLyrics ||
                state.showQueue ||
                state.queueProgress > 0f ||
                state.anchoredDraggableState.currentValue != DragValue.COLLAPSED ||
                state.anchoredDraggableState.targetValue != DragValue.COLLAPSED ||
                state.progress > 0.01f

        BackHandler(
            enabled = isBackHandlerEnabled
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

        val activity = context as? Activity
        val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }

        val widthClass = windowSizeClass?.widthSizeClass
        val heightClass = windowSizeClass?.heightSizeClass

        val isWideWindow = if (widthClass != null) {
            widthClass != WindowWidthSizeClass.Compact
        } else {
            screenWidth > screenHeight
        }

        val isCompactHeight = if (heightClass != null) {
            heightClass == WindowHeightSizeClass.Compact
        } else {
            screenHeight < 500.dp
        }

        val rightPaneWidth = if (isWideWindow) 64.dp else 0.dp
        val availableWidth = if (isWideWindow) screenWidth - rightPaneWidth else screenWidth
        val leftPaneWidth = if (isWideWindow) availableWidth * 0.5f else screenWidth

        val cornerRadius = lerp(50.dp, 0.dp, state.progress)
        val sheetShadowElevation = lerp(8.dp, 0.dp, state.progress)

        val targetRecordSize = when {
            isWideWindow -> min(screenHeight * 0.72f, leftPaneWidth * 0.85f)
            isCompactHeight -> min(screenHeight * 0.75f, 260.dp)
            else -> screenWidth * 0.85f
        }
        val recordSize = lerp(56.dp, targetRecordSize, state.progress)
        val artworkSize = lerp(56.dp, min(210.dp, recordSize * 0.6f), state.progress)

        val targetRecordX = when {
            isWideWindow -> leftPaneWidth + (leftPaneWidth - targetRecordSize) / 2
            else -> (screenWidth - targetRecordSize) / 2 - horizontalPadding
        }
        val recordX = lerp(screenWidth - recordSize - 8.dp - horizontalPadding, targetRecordX, state.progress)

        val targetRecordY = when {
            isWideWindow -> (screenHeight - targetRecordSize) / 2
            isCompactHeight -> screenHeight - (targetRecordSize * 0.5f)
            else -> screenHeight * 0.44f
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
                .padding(start = startPadding, end = endPadding)
                .anchoredDraggable(state.anchoredDraggableState, Orientation.Vertical)
        ) {
            NowPlayingScreenBackground(
                state = state,
                startPadding = startPadding,
                recordX = recordX,
                recordY = recordY,
                recordSize = recordSize,
                cornerRadius = cornerRadius,
                sheetShadowElevation = sheetShadowElevation
            ) {
                val controlsAlpha = ((state.progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                val expandedAlpha = ((state.progress - 0.05f) / 0.15f).coerceIn(0f, 1f)

                when {
                    isWideWindow -> {
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
                            leftPaneWidth = leftPaneWidth,
                            peekHeight = peekHeight,
                            overlayAlpha = overlayAlpha,
                            scrubFactor = scrubFactor,
                            controlsAlpha = controlsAlpha,
                            expandedAlpha = expandedAlpha,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onMiniPlayerPositioned = onMiniPlayerPositioned
                        )
                    }
                    isCompactHeight -> {
                        NowPlayingCompactContent(
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
                            overlayAlpha = overlayAlpha,
                            scrubFactor = scrubFactor,
                            controlsAlpha = controlsAlpha,
                            expandedAlpha = expandedAlpha,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onMiniPlayerPositioned = onMiniPlayerPositioned
                        )
                    }
                    else -> {
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
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onMiniPlayerPositioned = onMiniPlayerPositioned
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DismissIconButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onDismiss,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Dismiss",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NowPlayingCompactContent(
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
    overlayAlpha: Float,
    scrubFactor: Float,
    controlsAlpha: Float,
    expandedAlpha: Float,
    onAlbumClick: (String) -> Unit = {},
    onMiniPlayerPositioned: ((Offset) -> Unit)?,
    onArtistClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .displayCutoutPadding()
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
        if (state.progress > 0.05f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
                    .alpha(expandedAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Show action buttons and sheet dismiss button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NowPlayingActionRow(
                        repeatMode = playbackState.repeatMode,
                        onLyricsClick = { state.showLyrics = true },
                        onRepeatToggle = { playbackControls.onRepeatToggle() },
                        onQueueClick = { state.showQueue = true },
                        isCompact = true
                    )

                    DismissIconButton(
                        onDismiss = { state.collapse() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Show track title and artists (no album title) both 1 liner, track title will marquee and artists text will overflow with ellipses
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val displayArtist = remember(playbackState.artist, playbackState.trackTitle) {
                        collectArtistNames(playbackState.artist, playbackState.trackTitle).joinArtistsForDisplay()
                    }

                    Text(
                        text = playbackState.trackTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.showArtistsDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Show current timestamp bar
                if (state.progress > 0.8f) {
                    val displayPosition = if (state.isDraggingScrub) state.draggedPosition else progressState.positionMs
                    val (currentEnergy, currentEnergyDelta) = calculateEnergy(visualizerState, displayPosition)
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
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Collapsed View Content
        if (state.progress < 0.2f) {
            val miniPlayerAlpha = ((0.2f - state.progress) / 0.2f).coerceIn(0f, 1f)
            val isMiniState = state.effectiveExpandProgress == 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(peekHeight)
                    .alpha(miniPlayerAlpha)
                    .clickable {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    }
            ) {
                NowPlayingCollapsedView(
                    playbackState = playbackState,
                    progressState = progressState,
                    queueState = queueState,
                    actions = playbackControls,
                    showRecord = false,
                    rotation = { state.visualRotation },
                    textAlpha = { ((state.effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f) * state.effectiveTextAlpha },
                    onClick = {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    },
                    state = state
                )
            }
        }

        // 4. Show vinyl record, but only 50% rest is off screen below
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
            onArtistClick = onArtistClick
        )
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
    onAlbumClick: (String) -> Unit = {},
    onMiniPlayerPositioned: ((Offset) -> Unit)?,
    onArtistClick: (String) -> Unit
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
        // Track Content & Top Dismiss Row
        if (state.progress > 0.05f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .padding(horizontal = 24.dp)
                    .displayCutoutPadding()
                    .statusBarsPadding()
                    .alpha(expandedAlpha)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    NowPlayingTrackInfo(
                        modifier = Modifier.weight(1f),
                        playbackState = playbackState,
                        queueState = queueState,
                        pagerState = state.pagerState,
                        onAlbumClick = { albumId ->
                            state.collapse()
                            onAlbumClick(albumId)
                        }
                    ) {
                        state.showArtistsDialog = true
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    DismissIconButton(
                        onDismiss = { state.collapse() }
                    )
                }
            }
        }

        // Collapsed View Content
        if (state.progress < 0.2f) {
            val miniPlayerAlpha = ((0.2f - state.progress) / 0.2f).coerceIn(0f, 1f)
            val isMiniState = state.effectiveExpandProgress == 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(peekHeight)
                    .alpha(miniPlayerAlpha)
                    .clickable {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    }
            ) {
                NowPlayingCollapsedView(
                    playbackState = playbackState,
                    progressState = progressState,
                    queueState = queueState,
                    actions = playbackControls,
                    showRecord = false,
                    rotation = { state.visualRotation },
                    textAlpha = { ((state.effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f) * state.effectiveTextAlpha },
                    onClick = {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    },
                    state = state
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
            onArtistClick = onArtistClick
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
    leftPaneWidth: Dp,
    peekHeight: Dp,
    overlayAlpha: Float,
    scrubFactor: Float,
    controlsAlpha: Float,
    expandedAlpha: Float,
    onAlbumClick: (String) -> Unit = {},
    onMiniPlayerPositioned: ((Offset) -> Unit)?,
    onArtistClick: (String) -> Unit
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
        // Left Pane Column (Track Content & Waveform)
        if (state.progress > 0.05f) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(leftPaneWidth)
                    .padding(top = 16.dp)
                    .displayCutoutPadding()
                    .statusBarsPadding()
                    .alpha(expandedAlpha)
            ) {
                NowPlayingTrackInfo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    playbackState = playbackState,
                    queueState = queueState,
                    pagerState = state.pagerState,
                    titleMaxLines = 1,
                    onAlbumClick = { albumId ->
                        state.collapse()
                        onAlbumClick(albumId)
                    }
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
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // Collapsed View Content
        if (state.progress < 0.2f) {
            val miniPlayerAlpha = ((0.2f - state.progress) / 0.2f).coerceIn(0f, 1f)
            val isMiniState = state.effectiveExpandProgress == 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(peekHeight)
                    .alpha(miniPlayerAlpha)
                    .clickable {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    }
            ) {
                NowPlayingCollapsedView(
                    playbackState = playbackState,
                    progressState = progressState,
                    queueState = queueState,
                    actions = playbackControls,
                    showRecord = false,
                    rotation = { state.visualRotation },
                    textAlpha = { ((state.effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f) * state.effectiveTextAlpha },
                    onClick = {
                        if (isMiniState) {
                            if (playbackState.isPlaying) {
                                playbackControls.pause()
                            }
                            state.moveToState(DragValue.COLLAPSED)
                        } else {
                            state.expand()
                        }
                    },
                    state = state
                )
            }
        }

        // Action Buttons (Left Pane, bottom left)
        NowPlayingActionButtons(
            state = state,
            playbackState = playbackState,
            playbackControls = playbackControls,
            isLandscape = true
        )

        // Middle Pane: Vinyl Record Pager (Floating in Middle Pane)
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

        // Right Pane (Rightmost pane with Dismiss Sheet Icon)
        if (state.progress > 0.05f) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .displayCutoutPadding()
                    .statusBarsPadding()
                    .alpha(expandedAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DismissIconButton(
                    onDismiss = { state.collapse() }
                )
            }
        }

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
            onArtistClick = onArtistClick
        )
    }
}
