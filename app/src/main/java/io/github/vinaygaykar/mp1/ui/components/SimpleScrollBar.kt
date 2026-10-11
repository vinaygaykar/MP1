package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    thumbWidth: Dp = 4.dp,
    touchTargetWidth: Dp = 24.dp
) {
    val needsScrollbar by remember {
        derivedStateOf {
            listState.canScrollForward || listState.canScrollBackward
        }
    }

    if (!needsScrollbar) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeThumbColor = if (isDragging) primaryColor else thumbColor

    val animatedWidth by animateDpAsState(
        targetValue = if (isDragging) thumbWidth * 1.75f else thumbWidth,
        label = "thumb_width"
    )

    fun scrollToFraction(fraction: Float) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetIndex = (fraction.coerceIn(0f, 1f) * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(touchTargetWidth)
            .pointerInput(listState) {
                detectTapGestures { offset ->
                    if (size.height > 0) {
                        scrollToFraction(offset.y / size.height)
                    }
                }
            }
            .pointerInput(listState) {
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
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleScrollOffset = listState.firstVisibleItemScrollOffset

            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo

            if (totalItems > 0 && visibleItems.isNotEmpty()) {
                val visibleItemsCount = visibleItems.size
                val viewportHeight = size.height

                val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItemsCount.coerceAtLeast(1)
                val estimatedTotalHeight = averageItemSize * totalItems
                val currentScrollPx = (firstVisibleIndex * averageItemSize) + firstVisibleScrollOffset
                val maxScrollPx = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)

                val scrollProgress = (currentScrollPx / maxScrollPx).coerceIn(0f, 1f)

                val thumbHeight = (viewportHeight * (viewportHeight / estimatedTotalHeight.coerceAtLeast(viewportHeight)))
                    .coerceIn(32.dp.toPx(), viewportHeight)

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
}
