package io.github.vinaygaykar.mp1.ui.library

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.vinaygaykar.mp1.data.repo.ArtistImageRepository
import io.github.vinaygaykar.mp1.data.repo.SettingsRepository
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val artistImageRepository: ArtistImageRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val useCleanAlbumGrid: StateFlow<Boolean> = settingsRepository.useCleanAlbumGrid

    private var controller: MediaController? = null
    private var prefetchJob: Job? = null

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        }
    }

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val songs: StateFlow<List<Song>> = musicRepository.getAllSongs()
        .onEach { Log.d("LibraryViewModel", "Emitted ${it.size} songs") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = musicRepository.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = musicRepository.getAllArtists()
        .onEach { artistList ->
            val missingImages = artistList.filter { it.imageUrl.isNullOrEmpty() }.map { it.id to it.name }
            if (missingImages.isNotEmpty()) {
                prefetchJob?.cancel()
                prefetchJob = viewModelScope.launch {
                    artistImageRepository.prefetchArtistImages(missingImages)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<Genre>> = musicRepository.getAllGenres()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

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

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            val playlistId = playlistRepository.importPlaylist(uri)
            if (playlistId == -1L) {
                _importError.value = "Failed to import playlist. The selected file is invalid, unsupported, or contains no matching songs."
            }
        }
    }

    fun deletePlaylists(playlistIds: Set<Long>) {
        viewModelScope.launch {
            playlistIds.forEach { playlistId ->
                playlistRepository.deletePlaylist(playlistId)
            }
        }
    }

    suspend fun getArtistImageUrl(artistName: String, artistId: String): String? {
        return artistImageRepository.getArtistImageUrl(artistName, artistId)
    }

    private var playSongJob: Job? = null

    fun playSong(song: Song, songList: List<Song> = songs.value) {
        playSongJob?.cancel()
        playSongJob = viewModelScope.launch {
            val player = controller ?: return@launch
            val currentSongs = songList.ifEmpty { songs.value }
            if (currentSongs.isEmpty()) return@launch

            val startIndex = currentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            // 1. Immediately play the clicked song with minimal IPC overhead
            val targetMediaItem = withContext(Dispatchers.Default) {
                currentSongs[startIndex].toMediaItem()
            }
            player.setMediaItem(targetMediaItem)
            player.prepare()
            player.play()

            if (currentSongs.size <= 1) return@launch

            // 2. Asynchronously load the full queue in natural order (0..N-1) after UI animation window
            launch(Dispatchers.Default) {
                val allMediaItems = currentSongs.map { it.toMediaItem() }
                // Yield briefly to let initial click animation & miniplayer transition render smoothly
                delay(150)

                withContext(Dispatchers.Main) {
                    val activePlayer = controller ?: return@withContext
                    activePlayer.setMediaItems(
                        allMediaItems,
                        startIndex,
                        activePlayer.currentPosition
                    )
                }
            }
        }
    }

    fun addToQueue(song: Song) {
        controller?.addMediaItem(song.toMediaItem())
    }

    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
