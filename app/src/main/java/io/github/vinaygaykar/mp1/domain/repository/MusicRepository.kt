package io.github.vinaygaykar.mp1.domain.repository

import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getAlbumsByAlbumArtist(artistName: String): Flow<List<Album>>
    fun getFeaturedAlbumsForArtist(artistName: String): Flow<List<Album>>
    fun getAllArtists(): Flow<List<Artist>>
    fun getAllGenres(): Flow<List<Genre>>
    suspend fun getSongById(id: String): Song?
    fun searchSongs(query: String): Flow<List<Song>>
    fun searchAlbums(query: String): Flow<List<Album>>
    fun searchArtists(query: String): Flow<List<Artist>>
    fun getSongsByAlbum(albumId: String): Flow<List<Song>>
    fun getSongsByArtist(artistId: String): Flow<List<Song>>
    suspend fun getAlbumById(id: String): Album?
    fun getAlbumByIdFlow(id: String): Flow<Album?>
    suspend fun getArtistById(id: String): Artist?
    fun getArtistByIdFlow(id: String): Flow<Artist?>
    suspend fun getGenreById(id: String): Genre?
    fun getGenreByIdFlow(id: String): Flow<Genre?>
    fun getSongsByGenre(genreId: String): Flow<List<Song>>
    fun getCollaboratorsForArtist(artistName: String): Flow<List<Artist>>
    suspend fun getQueueSongsSync(): List<Song>
}
