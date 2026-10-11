package io.github.vinaygaykar.mp1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.vinaygaykar.mp1.data.local.entities.SongArtistCrossRef
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY COALESCE(discNumber, 1) ASC, COALESCE(trackNumber, 999) ASC, title ASC")
    fun getSongsByAlbum(albumId: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT songs.* FROM songs LEFT JOIN song_artists ON songs.id = song_artists.songId WHERE songs.artistId = :artistId OR song_artists.artistId = :artistId ORDER BY COALESCE(discNumber, 1) ASC, COALESCE(trackNumber, 999) ASC, title ASC")
    fun getSongsByArtist(artistId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE genreId = :genreId ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByGenre(genreId: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongArtistCrossRefs(refs: List<SongArtistCrossRef>)

    @Query("DELETE FROM songs WHERE scanFolderId = :scanFolderId")
    suspend fun deleteSongsByFolder(scanFolderId: String)

    @Query("SELECT * FROM songs WHERE scanFolderId = :scanFolderId")
    suspend fun getSongsByFolderSync(scanFolderId: String): List<SongEntity>

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteSongsByIds(songIds: List<String>)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query(
        """
        SELECT * FROM songs
        WHERE (
            :ftsQuery != '' AND rowid IN (SELECT docid FROM songs_fts WHERE songs_fts MATCH :ftsQuery)
        ) OR (
            title LIKE '%' || :rawQuery || '%'
        )
        ORDER BY
            CASE
                WHEN title LIKE :rawQuery || '%' THEN 1
                ELSE 2
            END,
            title COLLATE NOCASE ASC
    """
    )
    fun searchSongs(ftsQuery: String, rawQuery: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET lyrics = :lyrics, isLyricsSynced = :isSynced WHERE id = :songId")
    suspend fun updateLyrics(songId: String, lyrics: String?, isSynced: Boolean)
}
