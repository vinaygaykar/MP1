package io.github.vinaygaykar.mp1.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.client.PlaybackClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
    private val playback: PlaybackClient
) : ViewModel() {

    val playlistId: Long = savedStateHandle.get<Long>(PlaylistDetailActivity.EXTRA_PLAYLIST_ID)
        ?: (savedStateHandle.get<String>(PlaylistDetailActivity.EXTRA_PLAYLIST_ID)?.toLongOrNull() ?: 0L)

    val initialPlaylistName: String = savedStateHandle.get<String>(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME) ?: ""

    val playlist: StateFlow<Playlist?> = playlistRepository.getPlaylistByIdFlow(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<Song>> = playlistRepository.getSongsForPlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playSong(song: Song) {
        viewModelScope.launch { playback.play(songs.value, startAt = song) }
    }

    fun playPlaylist(shuffle: Boolean = false) {
        viewModelScope.launch { playback.play(songs.value, shuffle = shuffle) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { playback.addToQueue(song) }
    }
}
