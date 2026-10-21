package io.github.vinaygaykar.mp1.ui.genre

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import io.github.vinaygaykar.mp1.utils.traceSection

@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val genreId: String = savedStateHandle.get<String>(GenreDetailActivity.EXTRA_GENRE_ID) ?: ""
    val initialGenreName: String = savedStateHandle.get<String>(GenreDetailActivity.EXTRA_GENRE_NAME) ?: ""

    private var controller: MediaController? = null

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        }
    }

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
        viewModelScope.launch {
            val player = controller ?: return@launch
            val currentSongs = songs.value
            if (currentSongs.isEmpty()) {
                traceSection("player.setMediaItem_single") {
                    player.setMediaItem(song.toMediaItem())
                }
            } else {
                val mediaItems = withContext(Dispatchers.Default) {
                    traceSection("Song.toMediaItem_batch_${currentSongs.size}") {
                        currentSongs.map { it.toMediaItem() }
                    }
                }
                val startIndex = currentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                traceSection("player.setMediaItems_batch_${mediaItems.size}") {
                    player.setMediaItems(mediaItems, startIndex, 0L)
                }
            }
            player.prepare()
            player.play()
        }
    }

    fun playGenre(shuffle: Boolean = false) {
        viewModelScope.launch {
            val player = controller ?: return@launch
            val currentSongs = songs.value
            if (currentSongs.isEmpty()) return@launch

            val mediaItems = withContext(Dispatchers.Default) {
                traceSection("Song.toMediaItem_batch_${currentSongs.size}") {
                    currentSongs.map { it.toMediaItem() }
                }
            }
            player.shuffleModeEnabled = shuffle
            val startIndex = if (shuffle) mediaItems.indices.random() else 0
            traceSection("player.setMediaItems_batch_${mediaItems.size}") {
                player.setMediaItems(mediaItems, startIndex, 0L)
            }
            player.prepare()
            player.play()
        }
    }

    fun addToQueue(song: Song) {
        traceSection("controller.addMediaItem") {
            controller?.addMediaItem(song.toMediaItem())
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
