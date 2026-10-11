package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.vinaygaykar.mp1.data.local.entities.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE albumArtist = :artistName COLLATE NOCASE ORDER BY COALESCE(year, 0) DESC, name ASC")
    fun getAlbumsByAlbumArtist(artistName: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbumByIdFlow(id: String): Flow<AlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAll()

    @Query("UPDATE albums SET trackCount = (SELECT COUNT(*) FROM songs WHERE songs.albumId = albums.id)")
    suspend fun updateTrackCounts()

    @Query("DELETE FROM albums WHERE id NOT IN (SELECT DISTINCT albumId FROM songs WHERE albumId IS NOT NULL)")
    suspend fun deleteOrphanedAlbums()

    @Query(
        """
        SELECT * FROM albums
        WHERE name LIKE '%' || :rawQuery || '%' OR albumArtist LIKE '%' || :rawQuery || '%'
        ORDER BY
            CASE
                WHEN name LIKE :rawQuery || '%' THEN 1
                ELSE 2
            END,
            name COLLATE NOCASE ASC
    """
    )
    fun searchAlbums(rawQuery: String): Flow<List<AlbumEntity>>
}
