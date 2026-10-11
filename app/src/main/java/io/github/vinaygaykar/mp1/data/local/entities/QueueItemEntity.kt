package io.github.vinaygaykar.mp1.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_items",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QueueItemEntity(
    @PrimaryKey val queueItemId: String,
    val position: Int,
    val songId: String,
    val sourceContext: String?
)
