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
                    lastModified = entity.lastModified
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

        // 1. Validate file extension if present
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val supportedExtensions = setOf("m3u", "m3u8", "pls", "txt", "wpl")
        if (ext.isNotEmpty() && ext !in supportedExtensions) {
            return -1L
        }

        // 2. Read lines and check for binary / non-text content
        val songIdentifiers = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffered = inputStream.buffered()
                buffered.mark(2048)
                val buffer = ByteArray(2048)
                val readBytes = buffered.read(buffer)
                if (readBytes > 0) {
                    val sample = buffer.copyOf(readBytes)
                    if (isBinaryContent(sample)) {
                        return -1L
                    }
                }
                buffered.reset()

                buffered.bufferedReader().useLines { lines ->
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
            return -1L
        }

        if (songIdentifiers.isEmpty()) {
            return -1L
        }

        val allSongs = songDao.getAllSongs().first()
        val matchedPlaylistSongs = mutableListOf<PlaylistSongEntity>()
        var pos = 0

        val playlistName = fileName.substringBeforeLast(".")
            .ifBlank { "Imported Playlist" }

        var playlistId = -1L

        for (identifier in songIdentifiers) {
            val matchedSong = findMatchingSong(identifier, allSongs)
            if (matchedSong != null) {
                if (playlistId == -1L) {
                    playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = playlistName))
                }
                matchedPlaylistSongs.add(
                    PlaylistSongEntity(
                        playlistId = playlistId,
                        songId = matchedSong.id,
                        position = pos++
                    )
                )
            }
        }

        if (matchedPlaylistSongs.isNotEmpty() && playlistId != -1L) {
            playlistDao.insertPlaylistSongs(matchedPlaylistSongs)
        }

        return playlistId
    }

    private fun isBinaryContent(sample: ByteArray): Boolean {
        if (sample.size >= 3 && sample[0] == 0xFF.toByte() && sample[1] == 0xD8.toByte() && sample[2] == 0xFF.toByte()) {
            return true // JPEG
        }
        if (sample.size >= 4 && sample[0] == 0x89.toByte() && sample[1] == 0x50.toByte() && sample[2] == 0x4E.toByte() && sample[3] == 0x47.toByte()) {
            return true // PNG
        }
        if (sample.size >= 4 && sample[0] == 0x25.toByte() && sample[1] == 0x50.toByte() && sample[2] == 0x44.toByte() && sample[3] == 0x46.toByte()) {
            return true // PDF
        }
        if (sample.size >= 4 && sample[0] == 0x50.toByte() && sample[1] == 0x4B.toByte() && (sample[2] == 0x03.toByte() || sample[2] == 0x05.toByte())) {
            return true // ZIP / APK / JAR
        }
        if (sample.size >= 3 && sample[0] == 'I'.code.toByte() && sample[1] == 'D'.code.toByte() && sample[2] == '3'.code.toByte()) {
            return true // MP3 ID3 header
        }

        var nonPrintableCount = 0
        for (b in sample) {
            val v = b.toInt() and 0xFF
            if (v == 0) return true // Null byte indicates binary content
            if (v < 32 && v != 9 && v != 10 && v != 13) {
                nonPrintableCount++
            }
        }

        return (nonPrintableCount.toDouble() / sample.size) > 0.10
    }

    private fun findMatchingSong(
        identifier: String,
        allSongs: List<io.github.vinaygaykar.mp1.data.local.entities.SongEntity>
    ): io.github.vinaygaykar.mp1.data.local.entities.SongEntity? {
        val normalizedIdentifier = identifier.replace('\\', '/')
        val identifierFileName = normalizedIdentifier.substringAfterLast('/')
        val identifierNameNoExt = identifierFileName.substringBeforeLast('.')

        if (identifierFileName.isBlank()) return null

        return allSongs.find { song ->
            val songFileName = song.fileName
            val songFileNameNoExt = songFileName.substringBeforeLast('.')

            // 1. Match by full or partial URI
            song.uri.equals(identifier, ignoreCase = true) ||
            song.uri.contains(normalizedIdentifier, ignoreCase = true) ||

            // 2. Exact match on file name
            songFileName.equals(identifierFileName, ignoreCase = true) ||

            // 3. Match by filename without extension
            (identifierNameNoExt.isNotBlank() && songFileNameNoExt.equals(identifierNameNoExt, ignoreCase = true)) ||

            // 4. Relative path matching
            (song.relativePath.isNotBlank() && (
                normalizedIdentifier.contains(song.relativePath, ignoreCase = true) ||
                song.relativePath.contains(normalizedIdentifier, ignoreCase = true)
            )) ||

            // 5. Exact title match (only if identifier file name matches title exactly)
            (song.title.isNotBlank() && song.title.length >= 3 && identifierNameNoExt.equals(song.title, ignoreCase = true))
        }
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
