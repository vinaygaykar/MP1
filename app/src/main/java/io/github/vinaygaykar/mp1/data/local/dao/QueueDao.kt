package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.vinaygaykar.mp1.data.local.entities.QueueItemEntity
import kotlinx.coroutines.flow.Flow

import androidx.room.Transaction
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getQueue(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getQueueSync(): List<QueueItemEntity>

    @Query("SELECT songs.* FROM queue_items INNER JOIN songs ON queue_items.songId = songs.id ORDER BY queue_items.position ASC")
    suspend fun getQueueSongsSync(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Query("DELETE FROM queue_items WHERE queueItemId = :id")
    suspend fun removeItem(id: String)

    @Transaction
    suspend fun replaceQueue(items: List<QueueItemEntity>) {
        clearQueue()
        if (items.isNotEmpty()) {
            insertQueueItems(items)
        }
    }
}
