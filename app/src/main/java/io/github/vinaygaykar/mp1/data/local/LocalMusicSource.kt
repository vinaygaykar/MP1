package io.github.vinaygaykar.mp1.data.local

import android.net.Uri
import io.github.vinaygaykar.mp1.data.local.dao.AlbumDao
import io.github.vinaygaykar.mp1.data.local.dao.ArtistDao
import io.github.vinaygaykar.mp1.data.local.dao.GenreDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.data.local.entities.AlbumEntity
import io.github.vinaygaykar.mp1.data.local.entities.ArtistEntity
import io.github.vinaygaykar.mp1.data.local.entities.GenreEntity
import io.github.vinaygaykar.mp1.data.local.entities.SongEntity
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.getArtists
import io.github.vinaygaykar.mp1.utils.parseArtworkUri
import io.github.vinaygaykar.mp1.utils.toArtistId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import io.github.vinaygaykar.mp1.data.local.dao.QueueDao

class LocalMusicSource @Inject constructor(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val queueDao: QueueDao
) {
    suspend fun getQueueSongsSync(): List<Song> {
        return queueDao.getQueueSongsSync().map { it.toDomain() }
    }
    fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    fun getAllAlbums(): Flow<List<Album>> {
        return albumDao.getAllAlbums().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    fun getAllArtists(): Flow<List<Artist>> {
        return artistDao.getAllArtists().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    fun getAllGenres(): Flow<List<Genre>> {
        return genreDao.getAllGenres().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getSongById(id: String): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        val trimmed = query.trim()
        val ftsQuery = buildFtsQuery(trimmed)
        return songDao.searchSongs(ftsQuery = ftsQuery, rawQuery = trimmed).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchAlbums(query: String): Flow<List<Album>> {
        val trimmed = query.trim()
        return albumDao.searchAlbums(trimmed).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchArtists(query: String): Flow<List<Artist>> {
        val trimmed = query.trim()
        return artistDao.searchArtists(trimmed).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getSongsByAlbum(albumId: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAlbumsByAlbumArtist(artistName: String): Flow<List<Album>> {
        return albumDao.getAlbumsByAlbumArtist(artistName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFeaturedAlbumsForArtist(artistName: String): Flow<List<Album>> {
        return getAllSongs().combine(getAllAlbums()) { songs, albums ->
            val trimmedName = artistName.trim()
            if (trimmedName.isEmpty()) return@combine emptyList()

            val albumNamesWithFeaturedArtist = songs
                .filter { song ->
                    song.getArtists().any { it.equals(trimmedName, ignoreCase = true) }
                }
                .map { it.album.lowercase() }
                .toSet()

            albums.filter { album ->
                albumNamesWithFeaturedArtist.contains(album.name.lowercase()) &&
                        !album.artist.equals(trimmedName, ignoreCase = true)
            }
        }.flowOn(Dispatchers.Default)
    }

    fun getCollaboratorsForArtist(artistName: String): Flow<List<Artist>> {
        return getAllSongs().combine(getAllArtists()) { songs, artists ->
            val trimmedName = artistName.trim()
            if (trimmedName.isEmpty()) return@combine emptyList()

            val collaboratorMap = mutableMapOf<String, String>()
            songs
                .filter { song ->
                    song.getArtists().any { it.equals(trimmedName, ignoreCase = true) }
                }
                .flatMap { song -> song.getArtists() }
                .filter { name -> !name.equals(trimmedName, ignoreCase = true) }
                .forEach { name ->
                    if (!collaboratorMap.containsKey(name.lowercase())) {
                        collaboratorMap[name.lowercase()] = name
                    }
                }

            val artistMap = artists.associateBy { it.name.lowercase() }
            collaboratorMap.map { (lowerName, originalName) ->
                artistMap[lowerName] ?: Artist(
                    id = originalName.toArtistId(),
                    name = originalName,
                    trackCount = 0
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    fun getSongsByArtist(artistId: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAlbumById(id: String): Album? {
        return albumDao.getAlbumById(id)?.toDomain()
    }

    fun getAlbumByIdFlow(id: String): Flow<Album?> {
        return albumDao.getAlbumByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun getArtistById(id: String): Artist? {
        return artistDao.getArtistById(id)?.toDomain()
    }

    fun getArtistByIdFlow(id: String): Flow<Artist?> {
        return artistDao.getArtistByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun getGenreById(id: String): Genre? {
        return genreDao.getGenreById(id)?.toDomain()
    }

    fun getGenreByIdFlow(id: String): Flow<Genre?> {
        return genreDao.getGenreByIdFlow(id).map { it?.toDomain() }
    }

    fun getSongsByGenre(genreId: String): Flow<List<Song>> {
        return songDao.getSongsByGenre(genreId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun SongEntity.toDomain(): Song = Song(
        id = id,
        uri = Uri.parse(uri),
        title = title,
        artist = trackArtist,
        album = albumName,
        albumId = albumId,
        durationMs = durationMs,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        artworkUri = parseArtworkUri(artworkCachePath),
        fileName = fileName,
        lastModified = lastModified,
        artistsDisplayString = artistsDisplayString
    )

    private fun AlbumEntity.toDomain(): Album = Album(
        id = id,
        name = name,
        artist = albumArtist,
        year = year,
        artworkUri = parseArtworkUri(artworkCachePath),
        trackCount = trackCount
    )

    private fun ArtistEntity.toDomain(): Artist = Artist(
        id = id,
        name = name,
        trackCount = trackCount,
        imageUrl = imageUrl
    )

    private fun GenreEntity.toDomain(): Genre = Genre(
        id = id,
        name = name,
        trackCount = trackCount
    )

    companion object {
        private val FTS_QUERY_TOKEN_REGEX = Regex("""[\p{L}\p{N}]+""")

        fun buildFtsQuery(query: String): String {
            val tokens = FTS_QUERY_TOKEN_REGEX
                .findAll(query)
                .map { it.value.trim() }
                .filter { it.isNotEmpty() }
                .take(6)
                .toList()

            if (tokens.isEmpty()) return ""

            return tokens.joinToString(separator = " AND ") { "title:$it*" }
        }
    }
}
