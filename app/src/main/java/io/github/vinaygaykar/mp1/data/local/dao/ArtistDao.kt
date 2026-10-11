package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.vinaygaykar.mp1.data.local.entities.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE id = :id")
    fun getArtistByIdFlow(id: String): Flow<ArtistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists")
    suspend fun deleteAll()

    @Query("UPDATE artists SET trackCount = (SELECT COUNT(DISTINCT songId) FROM (SELECT id AS songId, artistId FROM songs WHERE artistId = artists.id UNION SELECT songId, artistId FROM song_artists WHERE artistId = artists.id))")
    suspend fun updateTrackCounts()

    @Query("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artistId FROM song_artists) AND id NOT IN (SELECT DISTINCT artistId FROM songs WHERE artistId IS NOT NULL)")
    suspend fun deleteOrphanedArtists()

    @Query("SELECT imageUrl FROM artists WHERE id = :id")
    suspend fun getArtistImageUrl(id: String): String?

    @Query("SELECT imageUrl FROM artists WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND imageUrl IS NOT NULL AND imageUrl != '' LIMIT 1")
    suspend fun getArtistImageUrlByNormalizedName(name: String): String?

    @Query("UPDATE artists SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateArtistImageUrl(id: String, imageUrl: String?)

    @Query(
        """
        SELECT * FROM artists
        WHERE name LIKE '%' || :rawQuery || '%'
        ORDER BY
            CASE
                WHEN name LIKE :rawQuery || '%' THEN 1
                ELSE 2
            END,
            name COLLATE NOCASE ASC
    """
    )
    fun searchArtists(rawQuery: String): Flow<List<ArtistEntity>>
}
