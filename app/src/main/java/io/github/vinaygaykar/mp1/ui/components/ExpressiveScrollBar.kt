package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

val LocalShowScrollbar = compositionLocalOf { true }

private data class ScrollMetrics(
    val progress: Float,
    val totalItemsCount: Int,
    val maxScrollIndex: Int,
    val scrollableHeight: Float
)

@Composable
fun ExpressiveScrollBar(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    minHeight: Dp = 48.dp,
    thickness: Dp = 8.dp,
    indicatorExpandedWidth: Dp = 24.dp,
    indicatorExpandedWidthBoost: Dp = 4.dp,
    indicatorRightCornerRadius: Dp = 6.dp,
    paddingEnd: Dp = 4.dp,
    trackGap: Dp = 8.dp,
    dragLabelProvider: ((Int) -> String?)? = null,
    dragLabelSize: Dp = 40.dp,
    dragLabelGap: Dp = 10.dp,
    onInteractingChanged: ((Boolean) -> Unit)? = null
) {
    if (!LocalShowScrollbar.current) return

    val canScrollForward by remember(listState, gridState) { derivedStateOf { listState?.canScrollForward ?: gridState?.canScrollForward ?: false } }
    val canScrollBackward by remember(listState, gridState) { derivedStateOf { listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false } }
    val canScroll = canScrollForward || canScrollBackward

    val listMetricsTracker = remember(listState) { AxisObservationTracker() }
    val gridMetricsTracker = remember(gridState) { AxisObservationTracker() }
    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(if (canScroll) expandedIndicatorWidth + paddingEnd else 0.dp)
    ) {
        if (!canScroll) return@BoxWithConstraints

        var isPressed by remember(listState, gridState) { mutableStateOf(false) }
        var isDragging by remember(listState, gridState) { mutableStateOf(false) }
        var dragProgress by remember(listState, gridState) { mutableFloatStateOf(-1f) }
        var pendingScrollIndex by remember(listState, gridState) { mutableIntStateOf(-1) }
        var retainedDragLabel by remember(listState, gridState) { mutableStateOf<String?>(null) }
        val displayedProgress = remember(listState, gridState) { Animatable(0f) }
        var hasSyncedDisplayedProgress by remember(listState, gridState) { mutableStateOf(false) }

        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceVariantColor = MaterialTheme.colorScheme.secondaryContainer
        val innerIcon = Icons.Rounded.UnfoldMore
        val indicatorRightCornerRadiusPx = with(LocalDensity.current) { indicatorRightCornerRadius.toPx() }

        val isInteracting = isPressed || isDragging

        LaunchedEffect(isInteracting) {
            onInteractingChanged?.invoke(isInteracting)
        }

        val animatedWidth by animateDpAsState(
            targetValue = if (isInteracting) expandedIndicatorWidth else thickness,
            animationSpec = tween(durationMillis = 200),
            label = "WidthAnimation"
        )

        val iconAlpha by animateFloatAsState(
            targetValue = if (isInteracting) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "IconAlpha"
        )

        val trackAlpha by animateFloatAsState(
            targetValue = if (isInteracting) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "TrackAlpha"
        )
        val density = LocalDensity.current
        val constraintsMaxWidth = maxWidth
        val constraintsMaxHeight = maxHeight
        val coarseJumpThresholdPx = with(density) { 16.dp.toPx() }
        val smoothJumpMinDistancePx = with(density) { 10.dp.toPx() }

        fun getScrollStats(): ScrollMetrics {
            val totalItemsCount: Int
            val currentScrollPx: Float
            val totalScrollableContentPx: Float
            val approximateMaxScrollIndex: Int

            val useGrid = (gridState != null && isInteracting) || (listState == null && gridState != null)

            if (!useGrid && listState != null) {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                totalItemsCount = layoutInfo.totalItemsCount

                if (visibleItems.isEmpty()) {
                    return ScrollMetrics(
                        progress = 0f,
                        totalItemsCount = totalItemsCount,
                        maxScrollIndex = 1,
                        scrollableHeight = 1f
                    )
                }

                observeListLayoutMetrics(layoutInfo, listMetricsTracker)

                val viewportHeightPx =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                        .coerceAtLeast(1f)
                val itemStridePx = listMetricsTracker.representativeStridePx(
                    fallbackStridePx = estimateListFallbackStridePx(
                        visibleItems = visibleItems,
                        spacingPx = layoutInfo.mainAxisItemSpacing
                    )
                )
                val representativeItemSizePx = listMetricsTracker.representativeItemSizePx(
                    fallbackItemSizePx = medianOrNull(visibleItems.map { it.size.toFloat() }) ?: 1f
                )
                val estimatedVisibleItems = (viewportHeightPx / itemStridePx).coerceAtLeast(1f)
                val lastItemIndex = (totalItemsCount - 1).coerceAtLeast(0)

                currentScrollPx = (
                    listMetricsTracker.distanceBeforeIndex(
                        index = listState.firstVisibleItemIndex,
                        representativeStridePx = itemStridePx
                    ) + listState.firstVisibleItemScrollOffset
                    ).coerceAtLeast(0f)
                totalScrollableContentPx = (
                    layoutInfo.beforeContentPadding +
                        layoutInfo.afterContentPadding
                    ).toFloat()
                    .plus(
                        listMetricsTracker.distanceBeforeIndex(
                            index = lastItemIndex,
                            representativeStridePx = itemStridePx
                        ) + listMetricsTracker.itemSizePx(
                            index = lastItemIndex,
                            representativeItemSizePx = representativeItemSizePx
                        ) - viewportHeightPx
                    )
                    .coerceAtLeast(1f)
                approximateMaxScrollIndex =
                    (totalItemsCount - estimatedVisibleItems).toInt().coerceAtLeast(1)
            } else if (gridState != null) {
                val layoutInfo = gridState.layoutInfo
                totalItemsCount = layoutInfo.totalItemsCount

                val visibleLines = observeGridLayoutMetrics(layoutInfo, gridMetricsTracker)
                val viewportHeightPx =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                        .coerceAtLeast(1f)
                val isVertical = layoutInfo.orientation == Orientation.Vertical
                val itemsPerRow = (layoutInfo.visibleItemsInfo.maxOfOrNull { if (isVertical) it.column else it.row }?.plus(1) ?: 1).coerceAtLeast(1)
                val totalRows = ((totalItemsCount + itemsPerRow - 1) / itemsPerRow).coerceAtLeast(1)
                val rowStridePx = gridMetricsTracker.representativeStridePx(
                    fallbackStridePx = estimateGridFallbackStridePx(
                        visibleLines = visibleLines,
                        spacingPx = layoutInfo.mainAxisItemSpacing
                    )
                )
                val representativeRowSizePx = gridMetricsTracker.representativeItemSizePx(
                    fallbackItemSizePx = medianOrNull(visibleLines.map { it.sizePx.toFloat() }) ?: 1f
                )
                val estimatedVisibleRows = (viewportHeightPx / rowStridePx).coerceAtLeast(1f)
                val currentRow = visibleLines.firstOrNull()?.index ?: 0
                val lastRowIndex = (totalRows - 1).coerceAtLeast(0)

                currentScrollPx = (
                    gridMetricsTracker.distanceBeforeIndex(
                        index = currentRow,
                        representativeStridePx = rowStridePx
                    ) + gridState.firstVisibleItemScrollOffset
                    ).coerceAtLeast(0f)
                totalScrollableContentPx = (
                    layoutInfo.beforeContentPadding +
                        layoutInfo.afterContentPadding
                    ).toFloat()
                    .plus(
                        gridMetricsTracker.distanceBeforeIndex(
                            index = lastRowIndex,
                            representativeStridePx = rowStridePx
                        ) + gridMetricsTracker.itemSizePx(
                            index = lastRowIndex,
                            representativeItemSizePx = representativeRowSizePx
                        ) - viewportHeightPx
                    )
                    .coerceAtLeast(1f)
                approximateMaxScrollIndex =
                    (((totalRows - estimatedVisibleRows).toInt().coerceAtLeast(1)) * itemsPerRow)
                        .coerceAtMost((totalItemsCount - 1).coerceAtLeast(1))
            } else {
                return ScrollMetrics(
                    progress = 0f,
                    totalItemsCount = 0,
                    maxScrollIndex = 1,
                    scrollableHeight = 1f
                )
            }

            if (totalItemsCount == 0) {
                return ScrollMetrics(
                    progress = 0f,
                    totalItemsCount = 0,
                    maxScrollIndex = 1,
                    scrollableHeight = 1f
                )
            }

            val forward = listState?.canScrollForward ?: gridState?.canScrollForward ?: false
            val backward = listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false

            val boundedScrollPx = currentScrollPx.coerceIn(0f, totalScrollableContentPx)
            val realProgress = if (!forward && totalItemsCount > 0) {
                1f
            } else if (!backward) {
                0f
            } else {
                (boundedScrollPx / totalScrollableContentPx).coerceIn(0f, 0.999f)
            }

            val availableHeight = with(density) { constraintsMaxHeight.toPx() }
            val handleHeightPx = with(density) { minHeight.toPx() }
            val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)

            return ScrollMetrics(
                progress = realProgress,
                totalItemsCount = totalItemsCount,
                maxScrollIndex = approximateMaxScrollIndex,
                scrollableHeight = scrollableHeight
            )
        }

        fun updateProgressFromTouch(touchY: Float, grabOffset: Float) {
            val stats = getScrollStats()
            val scrollableHeight = stats.scrollableHeight

            val targetHandleTop = touchY - grabOffset
            val newProgress = (targetHandleTop / scrollableHeight).coerceIn(0f, 1f)

            dragProgress = newProgress
            pendingScrollIndex = resolveDragTargetIndex(
                progress = newProgress,
                maxScrollIndex = stats.maxScrollIndex,
                totalItemsCount = stats.totalItemsCount
            )
        }

        LaunchedEffect(listState, gridState) {
            snapshotFlow { pendingScrollIndex }
                .distinctUntilChanged()
                .collectLatest { index ->
                    if (index >= 0) {
                        listState?.scrollToItem(index)
                        gridState?.scrollToItem(index)
                    }
                }
        }

        LaunchedEffect(listState, gridState, constraintsMaxHeight, minHeight, isDragging) {
            if (isDragging) return@LaunchedEffect

            snapshotFlow { getScrollStats() }
                .distinctUntilChanged()
                .collectLatest { stats ->
                    val targetProgress = stats.progress
                    if (!hasSyncedDisplayedProgress) {
                        displayedProgress.snapTo(targetProgress)
                        hasSyncedDisplayedProgress = true
                    } else {
                        val sourceIsScrolling =
                            listState?.isScrollInProgress == true ||
                                gridState?.isScrollInProgress == true
                        val handleDeltaPx =
                            abs(targetProgress - displayedProgress.value) * stats.scrollableHeight
                        val estimatedStepPx =
                            stats.scrollableHeight / stats.maxScrollIndex.coerceAtLeast(1).toFloat()
                        val shouldSmoothJump =
                            !sourceIsScrolling &&
                                estimatedStepPx >= coarseJumpThresholdPx &&
                                handleDeltaPx >= smoothJumpMinDistancePx

                        if (shouldSmoothJump) {
                            displayedProgress.animateTo(
                                targetValue = targetProgress,
                                animationSpec = tween(
                                    durationMillis = 70,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        } else {
                            displayedProgress.snapTo(targetProgress)
                        }
                    }
                }
        }

        LaunchedEffect(isDragging, dragProgress) {
            if (isDragging && dragProgress >= 0f) {
                displayedProgress.snapTo(dragProgress)
                hasSyncedDisplayedProgress = true
            }
        }

        val dragLabelTargetIndex = when {
            pendingScrollIndex >= 0 -> pendingScrollIndex
            gridState != null && isInteracting -> gridState.firstVisibleItemIndex
            listState != null -> listState.firstVisibleItemIndex
            gridState != null -> gridState.firstVisibleItemIndex
            else -> -1
        }
        val activeDragLabel =
            if (isDragging && dragLabelProvider != null && dragLabelTargetIndex >= 0) {
                dragLabelProvider(dragLabelTargetIndex)
            } else {
                null
            }
        val showDragLabel = isDragging && !activeDragLabel.isNullOrBlank()

        LaunchedEffect(activeDragLabel) {
            if (!activeDragLabel.isNullOrBlank()) {
                retainedDragLabel = activeDragLabel
            }
        }

        val dragLabelAlpha by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelAlpha"
        )
        val dragLabelScale by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0.82f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelScale"
        )
        val dragLabelSlide by animateDpAsState(
            targetValue = if (showDragLabel) 0.dp else 8.dp,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelSlide"
        )

        val indicatorPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var grabOffset = 0f

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true

                            val stats = getScrollStats()
                            val scrollableHeight = stats.scrollableHeight
                            val handleHeightPx = with(density) { minHeight.toPx() }

                            val visualProgress = displayedProgress.value
                            val handleY = visualProgress * scrollableHeight

                            val isTouchOnHandle = offset.y >= handleY && offset.y <= (handleY + handleHeightPx)

                            if (isTouchOnHandle) {
                                grabOffset = offset.y - handleY
                                dragProgress = visualProgress
                                pendingScrollIndex =
                                    listState?.firstVisibleItemIndex
                                        ?: gridState?.firstVisibleItemIndex
                                        ?: 0
                            } else {
                                grabOffset = handleHeightPx / 2f
                                updateProgressFromTouch(offset.y, grabOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateProgressFromTouch(change.position.y, grabOffset)
                        }
                    )
                }
        ) {
            val rightAnchorX = with(density) { (constraintsMaxWidth - paddingEnd).toPx() }
            val trackX = rightAnchorX - with(density) { thickness.toPx() / 2 }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val stats = getScrollStats()
                val scrollableHeight = stats.scrollableHeight

                val visualProgress = displayedProgress.value
                val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                val handleY = displayProgress * scrollableHeight
                val handleHeightPx = minHeight.toPx()

                val trackStrokeWidth = thickness.toPx()
                val indicatorWidthPx = animatedWidth.toPx()
                val gapPx = trackGap.toPx()
                val indicatorLeftCornerRadius = indicatorWidthPx / 2f
                val maxAllowedRightCornerRadius = minOf(indicatorWidthPx / 2f, handleHeightPx / 2f)
                val resolvedRightCornerRadius = indicatorRightCornerRadiusPx
                    .coerceIn(0f, maxAllowedRightCornerRadius)

                val currentIndicatorX = rightAnchorX - indicatorWidthPx

                if (trackAlpha > 0f) {
                    val trackColor = surfaceVariantColor.copy(alpha = surfaceVariantColor.alpha * trackAlpha)

                    if (handleY > gapPx) {
                        drawLine(
                            color = trackColor,
                            start = Offset(trackX, 0f),
                            end = Offset(trackX, handleY - gapPx),
                            strokeWidth = trackStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    if (handleY + handleHeightPx + gapPx < size.height) {
                        drawLine(
                            color = trackColor,
                            start = Offset(trackX, handleY + handleHeightPx + gapPx),
                            end = Offset(trackX, size.height),
                            strokeWidth = trackStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                indicatorPath.reset()
                indicatorPath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(currentIndicatorX, handleY),
                            size = Size(indicatorWidthPx, handleHeightPx)
                        ),
                        topLeft = CornerRadius(indicatorLeftCornerRadius, indicatorLeftCornerRadius),
                        topRight = CornerRadius(resolvedRightCornerRadius, resolvedRightCornerRadius),
                        bottomRight = CornerRadius(resolvedRightCornerRadius, resolvedRightCornerRadius),
                        bottomLeft = CornerRadius(indicatorLeftCornerRadius, indicatorLeftCornerRadius)
                    )
                )
                drawPath(
                    path = indicatorPath,
                    color = primaryColor
                )
            }
            
            if (iconAlpha > 0f) {
               Box(
                   modifier = Modifier
                       .offset {
                           val stats = getScrollStats()
                           val scrollableHeight = stats.scrollableHeight
                           val visualProgress = displayedProgress.value
                           val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                           val handleY = displayProgress * scrollableHeight
                           val handleHeightPx = with(density) { minHeight.toPx() }
                           
                           val iconSizePx = with(density) { 24.dp.toPx() }
                           val paddingEndPx = with(density) { paddingEnd.toPx() }
                           val animatedWidthPx = with(density) { animatedWidth.toPx() }
                           val maxWidthPx = with(density) { constraintsMaxWidth.toPx() }
                           
                           val x = maxWidthPx - paddingEndPx - (animatedWidthPx / 2) - (iconSizePx / 2)
                           val y = handleY + (handleHeightPx / 2) - (iconSizePx / 2)

                           IntOffset(x.toInt(), y.toInt())
                       }
                       .size(24.dp)
                       .graphicsLayer { 
                           alpha = iconAlpha 
                           scaleX = iconAlpha
                           scaleY = iconAlpha
                       }
               ) {
                   Icon(
                       imageVector = innerIcon,
                       contentDescription = null,
                       tint = MaterialTheme.colorScheme.onPrimary,
                       modifier = Modifier.fillMaxSize()
                   )
               }
            }

            val displayedDragLabel = activeDragLabel ?: retainedDragLabel
            if (dragLabelAlpha > 0f && !displayedDragLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .offset {
                            val stats = getScrollStats()
                            val scrollableHeight = stats.scrollableHeight
                            val visualProgress = displayedProgress.value
                            val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                            val handleY = displayProgress * scrollableHeight
                            val handleHeightPx = with(density) { minHeight.toPx() }
                            val dragLabelSizePx = with(density) { dragLabelSize.toPx() }
                            val dragLabelGapPx = with(density) { dragLabelGap.toPx() }
                            val dragLabelSlidePx = with(density) { dragLabelSlide.toPx() }
                            val paddingEndPx = with(density) { paddingEnd.toPx() }
                            val animatedWidthPx = with(density) { animatedWidth.toPx() }
                            val maxWidthPx = with(density) { constraintsMaxWidth.toPx() }

                            val indicatorX = maxWidthPx - paddingEndPx - animatedWidthPx
                            val x = indicatorX - dragLabelSizePx - dragLabelGapPx - dragLabelSlidePx
                            val y = handleY + (handleHeightPx / 2f) - (dragLabelSizePx / 2f)

                            androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
                        }
                        .size(dragLabelSize)
                        .graphicsLayer {
                            alpha = dragLabelAlpha
                            scaleX = dragLabelScale
                            scaleY = dragLabelScale
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayedDragLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
