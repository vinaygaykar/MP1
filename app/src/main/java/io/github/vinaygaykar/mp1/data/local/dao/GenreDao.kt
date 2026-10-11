package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.vinaygaykar.mp1.data.local.entities.GenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAllGenres(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genres WHERE id = :id")
    suspend fun getGenreById(id: String): GenreEntity?

    @Query("SELECT * FROM genres WHERE id = :id")
    fun getGenreByIdFlow(id: String): Flow<GenreEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Query("DELETE FROM genres")
    suspend fun deleteAll()

    @Query("UPDATE genres SET trackCount = (SELECT COUNT(*) FROM songs WHERE songs.genreId = genres.id)")
    suspend fun updateTrackCounts()

    @Query("DELETE FROM genres WHERE id NOT IN (SELECT DISTINCT genreId FROM songs WHERE genreId IS NOT NULL)")
    suspend fun deleteOrphanedGenres()
}
