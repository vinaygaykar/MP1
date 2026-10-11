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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun importPlaylist(uri: Uri) {
        viewModelScope.launch {
            playlistRepository.importPlaylist(uri)
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
            val totalSongs = currentSongs.size

            // 1. Immediately play the clicked song with minimal IPC overhead
            val targetMediaItem = withContext(Dispatchers.Default) {
                currentSongs[startIndex].toMediaItem()
            }
            player.setMediaItem(targetMediaItem)
            player.prepare()
            player.play()

            if (totalSongs <= 1) return@launch

            // 2. Asynchronously load the remaining queue in optimized background chunks
            launch(Dispatchers.Default) {
                // Yield briefly to let initial click animation & miniplayer transition render smoothly
                delay(100)

                val trailingSongs = currentSongs.subList(startIndex + 1, totalSongs)
                val leadingSongs = currentSongs.subList(0, startIndex)

                val chunkSize = 200 // Larger chunk size drastically reduces IPC roundtrips

                // Add trailing items first (songs after current)
                var trailingOffset = 0
                while (trailingOffset < trailingSongs.size) {
                    val chunkEnd = (trailingOffset + chunkSize).coerceAtMost(trailingSongs.size)
                    val chunkSongs = trailingSongs.subList(trailingOffset, chunkEnd)
                    val chunkMediaItems = chunkSongs.map { it.toMediaItem() }
                    withContext(Dispatchers.Main) {
                        val activePlayer = controller ?: return@withContext
                        activePlayer.addMediaItems(chunkMediaItems)
                    }
                    trailingOffset = chunkEnd
                    if (trailingOffset < trailingSongs.size) {
                        delay(50) // Yield to allow main thread UI rendering frame loop
                    }
                }

                // Add leading items next (songs before current)
                var leadingOffset = 0
                while (leadingOffset < leadingSongs.size) {
                    val chunkEnd = (leadingOffset + chunkSize).coerceAtMost(leadingSongs.size)
                    val chunkSongs = leadingSongs.subList(leadingOffset, chunkEnd)
                    val chunkMediaItems = chunkSongs.map { it.toMediaItem() }
                    val insertIndex = leadingOffset
                    withContext(Dispatchers.Main) {
                        val activePlayer = controller ?: return@withContext
                        activePlayer.addMediaItems(insertIndex, chunkMediaItems)
                    }
                    leadingOffset = chunkEnd
                    if (leadingOffset < leadingSongs.size) {
                        delay(50) // Yield to allow main thread UI rendering frame loop
                    }
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
