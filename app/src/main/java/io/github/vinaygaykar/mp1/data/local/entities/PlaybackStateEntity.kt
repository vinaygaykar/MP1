package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 0, // Singleton row
    val currentQueueItemId: String?,
    val positionMs: Long,
    val isShuffleOn: Boolean,
    val repeatMode: Int // OFF=0, ONE=1, ALL=2
)
