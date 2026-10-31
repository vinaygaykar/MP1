package io.github.vinaygaykar.mp1.ui.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.client.PlaybackClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
    private val playback: PlaybackClient
) : ViewModel() {

    val genreId: String = savedStateHandle.get<String>(GenreDetailActivity.EXTRA_GENRE_ID) ?: ""
    val initialGenreName: String = savedStateHandle.get<String>(GenreDetailActivity.EXTRA_GENRE_NAME) ?: ""

    val genre: StateFlow<Genre?> = musicRepository.getGenreByIdFlow(genreId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<Song>> = musicRepository.getSongsByGenre(genreId)
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

    fun playSong(song: Song) {
        viewModelScope.launch { playback.play(songs.value, startAt = song) }
    }

    fun playGenre(shuffle: Boolean = false) {
        viewModelScope.launch { playback.play(songs.value, shuffle = shuffle) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { playback.addToQueue(song) }
    }
}
