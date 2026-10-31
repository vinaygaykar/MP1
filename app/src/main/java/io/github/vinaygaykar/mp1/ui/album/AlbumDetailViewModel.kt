package io.github.vinaygaykar.mp1.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.getArtists
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.client.PlaybackClient
import io.github.vinaygaykar.mp1.utils.splitArtistsByDelimiters
import io.github.vinaygaykar.mp1.utils.toArtistId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
    private val playback: PlaybackClient
) : ViewModel() {

    val albumId: String = savedStateHandle.get<String>(AlbumDetailActivity.EXTRA_ALBUM_ID) ?: ""

    val album: StateFlow<Album?> = musicRepository.getAlbumByIdFlow(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val primaryArtist: StateFlow<Artist?> = combine(
        album,
        musicRepository.getAllArtists()
    ) { currentAlbum, allArtists ->
        if (currentAlbum == null) return@combine null
        val rawName = currentAlbum.artist.trim()
        if (rawName.isEmpty() || rawName == "Unknown Artist") return@combine null

        val cleanName = rawName.splitArtistsByDelimiters().firstOrNull() ?: rawName
        allArtists.find {
            it.name.equals(cleanName, ignoreCase = true) || it.name.equals(rawName, ignoreCase = true)
        } ?: Artist(
            id = cleanName.lowercase(),
            name = cleanName,
            trackCount = 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<Song>> = musicRepository.getSongsByAlbum(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songs)
        }
    }

    fun createPlaylistAndAddSongs(name: String, songs: List<Song>) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongsToPlaylist(playlistId, songs)
        }
    }

    val featuredArtists: StateFlow<List<Artist>> = combine(
        album,
        songs,
        musicRepository.getAllArtists()
    ) { currentAlbum, albumSongs, allArtists ->
        if (currentAlbum == null || albumSongs.isEmpty()) return@combine emptyList()

        val primaryArtistName = currentAlbum.artist.trim()
        val featuredArtistNames = albumSongs
            .flatMap { it.getArtists() }
            .map { it.trim() }
            .filter { name ->
                name.isNotEmpty() &&
                        !name.equals(primaryArtistName, ignoreCase = true) &&
                        !name.equals("Unknown Artist", ignoreCase = true)
            }
            .toSet()

        if (featuredArtistNames.isEmpty()) return@combine emptyList()

        val artistMap = allArtists.associateBy { it.name.lowercase() }
        featuredArtistNames.map { name ->
            artistMap[name.lowercase()] ?: Artist(
                id = name.toArtistId(),
                name = name,
                trackCount = albumSongs.count { song ->
                    song.getArtists().any { it.equals(name, ignoreCase = true) }
                }
            )
        }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playSong(song: Song) {
        viewModelScope.launch { playback.play(songs.value, startAt = song) }
    }

    fun playAlbum(shuffle: Boolean = false) {
        viewModelScope.launch { playback.play(songs.value, shuffle = shuffle) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { playback.addToQueue(song) }
    }
}
