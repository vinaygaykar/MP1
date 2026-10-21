package io.github.vinaygaykar.mp1.playback

import androidx.media3.common.Timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private lateinit var queueManager: QueueManager

    @Before
    fun setUp() {
        queueManager = QueueManager()
    }

    @Test
    fun buildQueueFromTimeline_whenEmptyTimeline_returnsEmptyQueueResult() {
        val emptyTimeline = Timeline.EMPTY
        val result = queueManager.buildQueueFromTimeline(
            timeline = emptyTimeline,
            isShuffleOn = false,
            currentWindowIndex = -1,
            currentWindowIndices = emptyList(),
            forceRebuild = false
        )

        assertTrue(result.items.isEmpty())
        assertTrue(result.windowIndices.isEmpty())
        assertEquals(-1, result.uiCurrentIndex)
    }
}
