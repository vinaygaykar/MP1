package io.github.vinaygaykar.mp1.data.repo

import io.github.vinaygaykar.mp1.data.local.LocalMusicSource
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    private val localMusicSource: LocalMusicSource
) : MusicRepository {
    override fun getAllSongs(): Flow<List<Song>> = localMusicSource.getAllSongs()
    override fun getAllAlbums(): Flow<List<Album>> = localMusicSource.getAllAlbums()
    override fun getAlbumsByAlbumArtist(artistName: String): Flow<List<Album>> = localMusicSource.getAlbumsByAlbumArtist(artistName)
    override fun getFeaturedAlbumsForArtist(artistName: String): Flow<List<Album>> = localMusicSource.getFeaturedAlbumsForArtist(artistName)
    override fun getAllArtists(): Flow<List<Artist>> = localMusicSource.getAllArtists()
    override fun getAllGenres(): Flow<List<Genre>> = localMusicSource.getAllGenres()
    override suspend fun getSongById(id: String): Song? = localMusicSource.getSongById(id)
    override fun searchSongs(query: String): Flow<List<Song>> = localMusicSource.searchSongs(query)
    override fun searchAlbums(query: String): Flow<List<Album>> = localMusicSource.searchAlbums(query)
    override fun searchArtists(query: String): Flow<List<Artist>> = localMusicSource.searchArtists(query)
    override fun getSongsByAlbum(albumId: String): Flow<List<Song>> = localMusicSource.getSongsByAlbum(albumId)
    override fun getSongsByArtist(artistId: String): Flow<List<Song>> = localMusicSource.getSongsByArtist(artistId)
    override suspend fun getAlbumById(id: String): Album? = localMusicSource.getAlbumById(id)
    override fun getAlbumByIdFlow(id: String): Flow<Album?> = localMusicSource.getAlbumByIdFlow(id)
    override suspend fun getArtistById(id: String): Artist? = localMusicSource.getArtistById(id)
    override fun getArtistByIdFlow(id: String): Flow<Artist?> = localMusicSource.getArtistByIdFlow(id)
    override suspend fun getGenreById(id: String): Genre? = localMusicSource.getGenreById(id)
    override fun getGenreByIdFlow(id: String): Flow<Genre?> = localMusicSource.getGenreByIdFlow(id)
    override fun getSongsByGenre(genreId: String): Flow<List<Song>> = localMusicSource.getSongsByGenre(genreId)
    override fun getCollaboratorsForArtist(artistName: String): Flow<List<Artist>> = localMusicSource.getCollaboratorsForArtist(artistName)
    override suspend fun getQueueSongsSync(): List<Song> = localMusicSource.getQueueSongsSync()
}
