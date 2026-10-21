package io.github.vinaygaykar.mp1.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

data class QueueBuildResult(
    val items: ImmutableList<MediaItem>,
    val windowIndices: List<Int>,
    val uiCurrentIndex: Int,
    val isShuffleOn: Boolean,
)

class QueueManager @Inject constructor() {

    fun buildQueueFromTimeline(
        timeline: Timeline,
        isShuffleOn: Boolean,
        currentWindowIndex: Int,
        currentWindowIndices: List<Int>,
        forceRebuild: Boolean
    ): QueueBuildResult {
        if (timeline.isEmpty) {
            return QueueBuildResult(
                items = persistentListOf(),
                windowIndices = emptyList(),
                uiCurrentIndex = -1,
                isShuffleOn = isShuffleOn
            )
        }

        val count = timeline.windowCount
        if (count == 0) {
            return QueueBuildResult(
                items = persistentListOf(),
                windowIndices = emptyList(),
                uiCurrentIndex = -1,
                isShuffleOn = isShuffleOn
            )
        }

        val shouldRebuild = forceRebuild ||
                currentWindowIndices.size != count ||
                currentWindowIndices.any { it !in 0 until count }

        if (shouldRebuild) {
            val window = Timeline.Window()
            val newQueue = ArrayList<MediaItem>(count)
            val newWindowIndices = ArrayList<Int>(count)

            if (isShuffleOn && count > 1) {
                val visited = BooleanArray(count)
                var visitedCount = 0

                var index = if (currentWindowIndex in 0 until count) {
                    currentWindowIndex
                } else {
                    timeline.getFirstWindowIndex(true)
                }

                while (index in 0 until count && !visited[index] && visitedCount < count) {
                    visited[index] = true
                    timeline.getWindow(index, window)
                    newQueue.add(window.mediaItem)
                    newWindowIndices.add(index)
                    visitedCount++
                    index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
                }

                if (visitedCount < count) {
                    var fallbackIndex = timeline.getFirstWindowIndex(true)
                    while (fallbackIndex in 0 until count && visitedCount < count) {
                        if (!visited[fallbackIndex]) {
                            visited[fallbackIndex] = true
                            timeline.getWindow(fallbackIndex, window)
                            newQueue.add(window.mediaItem)
                            newWindowIndices.add(fallbackIndex)
                            visitedCount++
                        }
                        fallbackIndex = timeline.getNextWindowIndex(fallbackIndex, Player.REPEAT_MODE_OFF, true)
                    }
                }

                if (visitedCount < count) {
                    for (i in 0 until count) {
                        if (!visited[i]) {
                            timeline.getWindow(i, window)
                            newQueue.add(window.mediaItem)
                            newWindowIndices.add(i)
                        }
                    }
                }
            } else {
                for (i in 0 until count) {
                    timeline.getWindow(i, window)
                    newQueue.add(window.mediaItem)
                    newWindowIndices.add(i)
                }
            }

            val uiCurrentIndex = if (currentWindowIndex >= 0 && newWindowIndices.isNotEmpty()) {
                val found = newWindowIndices.indexOf(currentWindowIndex)
                if (found != -1) found else currentWindowIndex.coerceIn(0, newWindowIndices.size - 1)
            } else {
                currentWindowIndex
            }

            return QueueBuildResult(
                items = newQueue.toPersistentList(),
                windowIndices = newWindowIndices,
                uiCurrentIndex = uiCurrentIndex,
                isShuffleOn = isShuffleOn
            )
        } else {
            val uiCurrentIndex = if (currentWindowIndex >= 0 && currentWindowIndices.isNotEmpty()) {
                val found = currentWindowIndices.indexOf(currentWindowIndex)
                if (found != -1) found else currentWindowIndex.coerceIn(0, currentWindowIndices.size - 1)
            } else {
                currentWindowIndex
            }

            return QueueBuildResult(
                items = persistentListOf(),
                windowIndices = currentWindowIndices,
                uiCurrentIndex = uiCurrentIndex,
                isShuffleOn = isShuffleOn
            )
        }
    }

    fun executePlayNext(
        player: Player,
        currentIdx: Int,
        indices: Set<Int>,
        windowIndices: List<Int>
    ) {
        val totalCount = player.mediaItemCount
        if (totalCount <= 1 || indices.isEmpty()) return

        val targetWindowIndices = indices.mapNotNull { windowIndices.getOrNull(it) }
            .filter { it in 0 until totalCount }.toSet()
        if (targetWindowIndices.isEmpty()) return

        if (player.shuffleModeEnabled) {
            player.shuffleModeEnabled = false
        }

        val activeCurrentIdx = if (currentIdx in 0 until totalCount) currentIdx else 0
        val selectedWithoutCurrent = targetWindowIndices.filter { it != activeCurrentIdx }.sorted()
        val unselected = (0 until totalCount).filter { it !in targetWindowIndices || it == activeCurrentIdx }

        val currentPosInUnselected = unselected.indexOf(activeCurrentIdx).let { if (it == -1) 0 else it }

        val targetList = mutableListOf<Int>()
        for (i in 0..currentPosInUnselected) {
            targetList.add(unselected[i])
        }
        targetList.addAll(selectedWithoutCurrent)
        for (i in (currentPosInUnselected + 1) until unselected.size) {
            targetList.add(unselected[i])
        }

        val workingList = (0 until totalCount).toMutableList()
        for (i in targetList.indices) {
            val targetVal = targetList[i]
            val fromIndex = (i until workingList.size).firstOrNull { workingList[it] == targetVal }
            if (fromIndex != null && fromIndex != i) {
                player.moveMediaItem(fromIndex, i)
                val item = workingList.removeAt(fromIndex)
                workingList.add(i, item)
            }
        }
    }
}
