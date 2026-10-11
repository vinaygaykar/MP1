package io.github.vinaygaykar.mp1.domain.repository

import android.net.Uri
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistByIdFlow(id: Long): Flow<Playlist?>
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String): Long
    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>)
    suspend fun importPlaylist(uri: Uri): Long
    suspend fun deletePlaylist(id: Long)
}
