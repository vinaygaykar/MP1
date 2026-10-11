package io.github.vinaygaykar.mp1.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo

internal class AxisObservationTracker {
    private var trackedTotalItemsCount = -1
    private var trackedSpacingPx = Int.MIN_VALUE
    private var representativeStridePx = 1f
    private var representativeItemSizePx = 1f
    private var representativeStrideSampleCount = 0
    private var representativeItemSizeSampleCount = 0
    private val observedStridesPx = mutableMapOf<Int, Float>()
    private val observedItemSizesPx = mutableMapOf<Int, Float>()

    fun resetIfNeeded(totalItemsCount: Int, spacingPx: Int) {
        if (trackedTotalItemsCount == totalItemsCount && trackedSpacingPx == spacingPx) {
            return
        }

        trackedTotalItemsCount = totalItemsCount
        trackedSpacingPx = spacingPx
        representativeStridePx = 1f
        representativeItemSizePx = 1f
        representativeStrideSampleCount = 0
        representativeItemSizeSampleCount = 0
        observedStridesPx.clear()
        observedItemSizesPx.clear()
    }

    fun observeRepresentativeSample(
        strideSamplePx: Float?,
        itemSizeSamplePx: Float?
    ) {
        val normalizedStrideSamplePx = strideSamplePx?.coerceAtLeast(1f)
        val normalizedItemSizeSamplePx = itemSizeSamplePx?.coerceAtLeast(1f)

        if (normalizedStrideSamplePx == null && normalizedItemSizeSamplePx == null) {
            return
        }

        normalizedStrideSamplePx?.let { sample ->
            val currentCount = representativeStrideSampleCount
            representativeStrideSampleCount = currentCount + 1
            representativeStridePx =
                if (currentCount == 0) {
                    sample
                } else {
                    ((representativeStridePx * currentCount) + sample) / representativeStrideSampleCount
                }
        }

        normalizedItemSizeSamplePx?.let { sample ->
            val currentCount = representativeItemSizeSampleCount
            representativeItemSizeSampleCount = currentCount + 1
            representativeItemSizePx =
                if (currentCount == 0) {
                    sample
                } else {
                    ((representativeItemSizePx * currentCount) + sample) / representativeItemSizeSampleCount
                }
        }
    }

    fun observeItemSize(index: Int, sizePx: Float) {
        observedItemSizesPx[index] = sizePx.coerceAtLeast(1f)
    }

    fun observeStride(index: Int, stridePx: Float) {
        observedStridesPx[index] = stridePx.coerceAtLeast(1f)
    }

    fun representativeStridePx(fallbackStridePx: Float): Float {
        return if (representativeStrideSampleCount > 0) {
            representativeStridePx
        } else {
            fallbackStridePx.coerceAtLeast(1f)
        }
    }

    fun representativeItemSizePx(fallbackItemSizePx: Float): Float {
        return if (representativeItemSizeSampleCount > 0) {
            representativeItemSizePx
        } else {
            fallbackItemSizePx.coerceAtLeast(1f)
        }
    }

    fun distanceBeforeIndex(index: Int, representativeStridePx: Float): Float {
        if (index <= 0) return 0f

        var correctionPx = 0f
        observedStridesPx.forEach { (observedIndex, observedStridePx) ->
            if (observedIndex < index) {
                correctionPx += observedStridePx - representativeStridePx
            }
        }

        return ((index * representativeStridePx) + correctionPx).coerceAtLeast(0f)
    }

    fun itemSizePx(index: Int, representativeItemSizePx: Float): Float {
        return observedItemSizesPx[index] ?: representativeItemSizePx.coerceAtLeast(1f)
    }
}

internal fun estimateListFallbackStridePx(
    visibleItems: List<LazyListItemInfo>,
    spacingPx: Int
): Float {
    val strideSamples = visibleItems
        .zipWithNext()
        .mapNotNull { (current, next) ->
            (next.offset - current.offset)
                .takeIf { next.index == current.index + 1 && it > 0 }
                ?.toFloat()
        }

    return medianOrNull(strideSamples)
        ?: medianOrNull(visibleItems.map { it.size.toFloat() + spacingPx })
        ?: 1f
}

internal fun observeListLayoutMetrics(
    layoutInfo: LazyListLayoutInfo,
    tracker: AxisObservationTracker
) {
    tracker.resetIfNeeded(
        totalItemsCount = layoutInfo.totalItemsCount,
        spacingPx = layoutInfo.mainAxisItemSpacing
    )

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    tracker.observeRepresentativeSample(
        strideSamplePx = estimateListFallbackStridePx(
            visibleItems = visibleItems,
            spacingPx = layoutInfo.mainAxisItemSpacing
        ),
        itemSizeSamplePx = medianOrNull(visibleItems.map { it.size.toFloat() })
    )

    visibleItems.forEach { item ->
        tracker.observeItemSize(index = item.index, sizePx = item.size.toFloat())
    }

    visibleItems
        .zipWithNext()
        .forEach { (current, next) ->
            if (next.index == current.index + 1) {
                tracker.observeStride(
                    index = current.index,
                    stridePx = (next.offset - current.offset).toFloat()
                )
            }
        }

    val lastVisibleItem = visibleItems.last()
    if (lastVisibleItem.index < layoutInfo.totalItemsCount - 1) {
        tracker.observeStride(
            index = lastVisibleItem.index,
            stridePx = (lastVisibleItem.size + layoutInfo.mainAxisItemSpacing).toFloat()
        )
    }
}

internal data class VisibleGridLineMetrics(
    val index: Int,
    val offsetPx: Int,
    val sizePx: Int
)

internal fun buildVisibleGridLines(layoutInfo: LazyGridLayoutInfo): List<VisibleGridLineMetrics> {
    val isVertical =
        layoutInfo.orientation == Orientation.Vertical
    val groupedLines = linkedMapOf<Int, MutableList<LazyGridItemInfo>>()

    layoutInfo.visibleItemsInfo.forEach { item ->
        val lineIndex = if (isVertical) item.row else item.column
        if (lineIndex >= 0) {
            groupedLines.getOrPut(lineIndex) { mutableListOf() }.add(item)
        }
    }

    return groupedLines
        .entries
        .map { (lineIndex, itemsInLine) ->
            VisibleGridLineMetrics(
                index = lineIndex,
                offsetPx = itemsInLine.minOf { if (isVertical) it.offset.y else it.offset.x },
                sizePx = itemsInLine.maxOf { if (isVertical) it.size.height else it.size.width }
            )
        }
        .sortedBy { it.index }
}

internal fun estimateGridFallbackStridePx(
    visibleLines: List<VisibleGridLineMetrics>,
    spacingPx: Int
): Float {
    val strideSamples = visibleLines
        .zipWithNext()
        .mapNotNull { (current, next) ->
            (next.offsetPx - current.offsetPx)
                .takeIf { next.index == current.index + 1 && it > 0 }
                ?.toFloat()
        }

    return medianOrNull(strideSamples)
        ?: medianOrNull(visibleLines.map { it.sizePx.toFloat() + spacingPx })
        ?: 1f
}

internal fun observeGridLayoutMetrics(
    layoutInfo: LazyGridLayoutInfo,
    tracker: AxisObservationTracker
): List<VisibleGridLineMetrics> {
    tracker.resetIfNeeded(
        totalItemsCount = layoutInfo.totalItemsCount,
        spacingPx = layoutInfo.mainAxisItemSpacing
    )

    val visibleLines = buildVisibleGridLines(layoutInfo)
    if (visibleLines.isEmpty()) return visibleLines

    tracker.observeRepresentativeSample(
        strideSamplePx = estimateGridFallbackStridePx(
            visibleLines = visibleLines,
            spacingPx = layoutInfo.mainAxisItemSpacing
        ),
        itemSizeSamplePx = medianOrNull(visibleLines.map { it.sizePx.toFloat() })
    )

    visibleLines.forEach { line ->
        tracker.observeItemSize(index = line.index, sizePx = line.sizePx.toFloat())
    }

    visibleLines
        .zipWithNext()
        .forEach { (current, next) ->
            if (next.index == current.index + 1) {
                tracker.observeStride(
                    index = current.index,
                    stridePx = (next.offsetPx - current.offsetPx).toFloat()
                )
            }
        }

    val isVertical = layoutInfo.orientation == Orientation.Vertical
    val itemsPerRow = (layoutInfo.visibleItemsInfo.maxOfOrNull { if (isVertical) it.column else it.row }?.plus(1) ?: 1).coerceAtLeast(1)
    val totalLines = ((layoutInfo.totalItemsCount + itemsPerRow - 1) / itemsPerRow)
        .coerceAtLeast(1)
    val lastVisibleLine = visibleLines.last()
    if (lastVisibleLine.index < totalLines - 1) {
        tracker.observeStride(
            index = lastVisibleLine.index,
            stridePx = (lastVisibleLine.sizePx + layoutInfo.mainAxisItemSpacing).toFloat()
        )
    }

    return visibleLines
}

internal fun resolveDragTargetIndex(
    progress: Float,
    maxScrollIndex: Int,
    totalItemsCount: Int
): Int {
    if (totalItemsCount <= 1) return 0

    val clampedProgress = progress.coerceIn(0f, 1f)
    val lastIndex = totalItemsCount - 1
    if (clampedProgress >= 1f) return lastIndex

    return (clampedProgress * maxScrollIndex.coerceAtLeast(1))
        .toInt()
        .coerceIn(0, lastIndex)
}

internal fun extractFastScrollGlyph(value: String?): String? {
    val leadingChar = value
        .orEmpty()
        .trim()
        .firstOrNull { it.isLetterOrDigit() }
        ?: return null

    return if (leadingChar.isDigit()) {
        "#"
    } else {
        leadingChar.uppercaseChar().toString()
    }
}

internal fun medianOrNull(values: Iterable<Float>): Float? {
    val sorted = values
        .filter { it.isFinite() && it > 0f }
        .sorted()

    if (sorted.isEmpty()) return null

    val middleIndex = sorted.size / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middleIndex - 1] + sorted[middleIndex]) / 2f
    } else {
        sorted[middleIndex]
    }
}
