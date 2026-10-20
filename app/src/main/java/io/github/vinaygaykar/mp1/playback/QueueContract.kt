package io.github.vinaygaykar.mp1.playback

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class QueueUiState(
    val items: ImmutableList<MediaItem> = persistentListOf(),
    val currentIndex: Int = -1,
)

interface QueueControls {
    fun addToQueue(mediaItem: MediaItem)
    fun onSkipToQueueItem(index: Int)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue()
    fun removeIndicesFromQueue(indices: Set<Int>) {}
    fun removeOthersFromQueue(indicesToKeep: Set<Int>) {}
    fun playNextInQueue(indices: Set<Int>) {}
}
