package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SimpleScrollBar(
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    thumbWidth: Dp = 4.dp,
    touchTargetWidth: Dp = 24.dp,
    paddingEnd: Dp = 6.dp,
    onInteractingChanged: ((Boolean) -> Unit)? = null
) {
    val needsScrollbar by remember(listState, gridState) {
        derivedStateOf {
            (listState?.canScrollForward == true || listState?.canScrollBackward == true) ||
            (gridState?.canScrollForward == true || gridState?.canScrollBackward == true)
        }
    }

    if (!needsScrollbar) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isDragging) {
        onInteractingChanged?.invoke(isDragging)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeThumbColor = if (isDragging) primaryColor else thumbColor

    val animatedWidth by animateDpAsState(
        targetValue = if (isDragging) thumbWidth * 1.75f else thumbWidth,
        label = "thumb_width"
    )

    fun scrollToFraction(fraction: Float) {
        val totalItems = listState?.layoutInfo?.totalItemsCount
            ?: gridState?.layoutInfo?.totalItemsCount
            ?: 0
        if (totalItems > 0) {
            val targetIndex = (fraction.coerceIn(0f, 1f) * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
            coroutineScope.launch {
                listState?.scrollToItem(targetIndex)
                gridState?.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = paddingEnd)
            .width(touchTargetWidth)
            .pointerInput(listState, gridState) {
                detectTapGestures { offset ->
                    if (size.height > 0) {
                        scrollToFraction(offset.y / size.height)
                    }
                }
            }
            .pointerInput(listState, gridState) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        if (size.height > 0) {
                            scrollToFraction(offset.y / size.height)
                        }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        if (size.height > 0) {
                            scrollToFraction(change.position.y / size.height)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(animatedWidth)
        ) {
            val totalItems: Int
            val firstVisibleIndex: Int
            val firstVisibleScrollOffset: Int
            val estimatedTotalHeight: Float
            val currentScrollPx: Float

            if (listState != null) {
                val layoutInfo = listState.layoutInfo
                totalItems = layoutInfo.totalItemsCount
                val visibleItems = layoutInfo.visibleItemsInfo
                if (totalItems == 0 || visibleItems.isEmpty()) return@Canvas

                firstVisibleIndex = listState.firstVisibleItemIndex
                firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset

                val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size.coerceAtLeast(1)
                estimatedTotalHeight = averageItemSize * totalItems
                currentScrollPx = (firstVisibleIndex * averageItemSize) + firstVisibleScrollOffset
            } else if (gridState != null) {
                val layoutInfo = gridState.layoutInfo
                totalItems = layoutInfo.totalItemsCount
                val visibleItems = layoutInfo.visibleItemsInfo
                if (totalItems == 0 || visibleItems.isEmpty()) return@Canvas

                firstVisibleIndex = gridState.firstVisibleItemIndex
                firstVisibleScrollOffset = gridState.firstVisibleItemScrollOffset

                val numColumns = visibleItems.maxOfOrNull { it.column + 1 }?.coerceAtLeast(1) ?: 1
                val totalRows = (totalItems + numColumns - 1) / numColumns
                val currentRow = firstVisibleIndex / numColumns
                val averageRowSize = visibleItems.sumOf { it.size.height }.toFloat() / visibleItems.size.coerceAtLeast(1)

                estimatedTotalHeight = averageRowSize * totalRows
                currentScrollPx = (currentRow * averageRowSize) + firstVisibleScrollOffset
            } else {
                return@Canvas
            }

            val viewportHeight = size.height
            if (viewportHeight <= 0f) return@Canvas

            val maxScrollPx = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
            val scrollProgress = (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)

            val minThumbHeight = 32.dp.toPx().coerceAtMost(viewportHeight)
            val maxThumbHeight = viewportHeight

            val thumbHeight = (viewportHeight * (viewportHeight / estimatedTotalHeight.coerceAtLeast(viewportHeight)))
                .coerceIn(minThumbHeight, maxThumbHeight)

            val thumbOffsetY = (viewportHeight - thumbHeight) * scrollProgress

            drawRoundRect(
                color = activeThumbColor,
                topLeft = Offset(0f, thumbOffsetY),
                size = Size(animatedWidth.toPx(), thumbHeight),
                cornerRadius = CornerRadius(animatedWidth.toPx() / 2f)
            )
        }
    }
}
