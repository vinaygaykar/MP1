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
    val uiCurrentIndex: Int,
    val isShuffleOn: Boolean,
)

class QueueManager @Inject constructor() {

    /**
     * The queue shown in the UI is the player's playlist, in order (shuffle reorders the playlist
     * itself, so there is no separate shuffled view). [QueueBuildResult.items] is empty when the
     * current list is still valid and only the current index changed.
     */
    fun buildQueueFromTimeline(
        timeline: Timeline,
        isShuffleOn: Boolean,
        currentWindowIndex: Int,
        currentQueueSize: Int,
        forceRebuild: Boolean
    ): QueueBuildResult {
        val count = if (timeline.isEmpty) 0 else timeline.windowCount
        if (count == 0) {
            return QueueBuildResult(persistentListOf(), uiCurrentIndex = -1, isShuffleOn = isShuffleOn)
        }

        val uiCurrentIndex = if (currentWindowIndex >= 0) currentWindowIndex.coerceAtMost(count - 1) else currentWindowIndex

        if (!forceRebuild && currentQueueSize == count) {
            return QueueBuildResult(persistentListOf(), uiCurrentIndex, isShuffleOn)
        }

        val window = Timeline.Window()
        val items = (0 until count).map { timeline.getWindow(it, window).mediaItem }
        return QueueBuildResult(items.toPersistentList(), uiCurrentIndex, isShuffleOn)
    }

    fun executePlayNext(
        player: Player,
        currentIdx: Int,
        indices: Set<Int>
    ) {
        val totalCount = player.mediaItemCount
        if (totalCount <= 1 || indices.isEmpty()) return

        val targetWindowIndices = indices.filter { it in 0 until totalCount }.toSet()
        if (targetWindowIndices.isEmpty()) return

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
