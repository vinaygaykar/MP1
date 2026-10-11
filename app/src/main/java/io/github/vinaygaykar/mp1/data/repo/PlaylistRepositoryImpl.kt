package io.github.vinaygaykar.mp1.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.vinaygaykar.mp1.data.local.dao.PlaylistDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.data.local.entities.PlaylistEntity
import io.github.vinaygaykar.mp1.data.local.entities.PlaylistSongEntity
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.utils.parseArtworkUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun getPlaylistByIdFlow(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistByIdFlow(id).map { entity ->
            entity?.let {
                Playlist(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.id,
                    uri = Uri.parse(entity.uri),
                    title = entity.title,
                    artist = entity.trackArtist,
                    album = entity.albumName,
                    albumId = entity.albumId,
                    durationMs = entity.durationMs,
                    trackNumber = entity.trackNumber,
                    discNumber = entity.discNumber,
                    year = entity.year,
                    artworkUri = parseArtworkUri(entity.artworkCachePath),
                    fileName = entity.fileName,
                    lastModified = entity.lastModified,
                    lyrics = entity.lyrics,
                    isLyricsSynced = entity.isLyricsSynced
                )
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val entity = PlaylistEntity(name = name)
        return playlistDao.insertPlaylist(entity)
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        val existingSongs = playlistDao.getSongsForPlaylist(playlistId).first()
        val existingIds = existingSongs.map { it.id }.toSet()
        val newSongs = songs.filterNot { existingIds.contains(it.id) }
        var position = existingSongs.size
        val entities = newSongs.map { song ->
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                position = position++
            )
        }
        if (entities.isNotEmpty()) {
            playlistDao.insertPlaylistSongs(entities)
        }
    }

    override suspend fun importPlaylist(uri: Uri): Long {
        val fileName = getFileNameFromUri(uri) ?: "Imported Playlist"
        val playlistName = fileName.substringBeforeLast(".")
            .ifBlank { "Imported Playlist" }

        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = playlistName))

        // Read lines from imported playlist file (M3U, PLS, TXT, etc.)
        val songIdentifiers = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.isNotBlank() && !line.startsWith("#")) {
                            songIdentifiers.add(line)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songIdentifiers.isNotEmpty()) {
            val allSongs = songDao.getAllSongs().first()
            val matchedPlaylistSongs = mutableListOf<PlaylistSongEntity>()
            var pos = 0

            for (identifier in songIdentifiers) {
                val matchedSong = allSongs.find { song ->
                    song.uri.contains(identifier, ignoreCase = true) ||
                            identifier.contains(song.title, ignoreCase = true) ||
                            song.relativePath.contains(identifier, ignoreCase = true) ||
                            song.fileName.contains(identifier, ignoreCase = true)
                }
                if (matchedSong != null) {
                    matchedPlaylistSongs.add(
                        PlaylistSongEntity(
                            playlistId = playlistId,
                            songId = matchedSong.id,
                            position = pos++
                        )
                    )
                }
            }

            if (matchedPlaylistSongs.isNotEmpty()) {
                playlistDao.insertPlaylistSongs(matchedPlaylistSongs)
            }
        }

        return playlistId
    }

    override suspend fun deletePlaylist(id: Long) {
        playlistDao.deletePlaylistById(id)
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }
}
