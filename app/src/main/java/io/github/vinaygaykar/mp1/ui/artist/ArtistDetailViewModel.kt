package io.github.vinaygaykar.mp1.ui.artist

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import io.github.vinaygaykar.mp1.data.network.models.DeezerArtist
import io.github.vinaygaykar.mp1.data.repo.ArtistImageRepository
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.PlaybackService
import io.github.vinaygaykar.mp1.utils.traceSection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistImageRepository: ArtistImageRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val artistId: String = savedStateHandle.get<String>(ArtistDetailActivity.EXTRA_ARTIST_ID) ?: ""

    private var controller: MediaController? = null

    private val _candidateImages = MutableStateFlow<List<DeezerArtist>>(emptyList())
    val candidateImages: StateFlow<List<DeezerArtist>> = _candidateImages.asStateFlow()

    private val _isSearchingImages = MutableStateFlow(false)
    val isSearchingImages: StateFlow<Boolean> = _isSearchingImages.asStateFlow()

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val hints = Bundle().apply { putBoolean("NEEDS_TIMELINE", false) }
            controller = MediaController.Builder(context, sessionToken)
                .setConnectionHints(hints)
                .buildAsync().await()
        }
    }

    val artist: StateFlow<Artist?> = musicRepository.getArtistByIdFlow(artistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val artistImageUrl: StateFlow<String?> = artist
        .flatMapLatest { currentArtist ->
            if (currentArtist != null && currentArtist.name.isNotBlank()) {
                flow<String?> {
                    val cachedOrFetchedUrl = currentArtist.imageUrl
                        ?: artistImageRepository.getArtistImageUrl(
                            artistName = currentArtist.name,
                            artistId = currentArtist.id,
                            ignoreFailedCache = true
                        )
                    emit(cachedOrFetchedUrl)
                }
            } else {
                flowOf<String?>(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val albums: StateFlow<List<Album>> = artist
        .flatMapLatest { currentArtist ->
            if (currentArtist != null && currentArtist.name.isNotBlank()) {
                musicRepository.getAlbumsByAlbumArtist(currentArtist.name)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val featuredAlbums: StateFlow<List<Album>> = artist
        .flatMapLatest { currentArtist ->
            if (currentArtist != null && currentArtist.name.isNotBlank()) {
                musicRepository.getFeaturedAlbumsForArtist(currentArtist.name)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val songs: StateFlow<List<Song>> = musicRepository.getSongsByArtist(artistId)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val collaborators: StateFlow<List<Artist>> = artist
        .flatMapLatest { currentArtist ->
            if (currentArtist != null && currentArtist.name.isNotBlank()) {
                musicRepository.getCollaboratorsForArtist(currentArtist.name)
            } else {
                flowOf(emptyList())
            }
        }
        .onEach { collabs ->
            val missing = collabs.filter { it.imageUrl.isNullOrEmpty() }.map { it.id to it.name }
            if (missing.isNotEmpty()) {
                viewModelScope.launch {
                    artistImageRepository.prefetchArtistImages(missing)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getArtistImageUrl(artistName: String, artistId: String, ignoreFailedCache: Boolean = true): String? {
        return artistImageRepository.getArtistImageUrl(artistName, artistId, ignoreFailedCache = ignoreFailedCache)
    }

    fun searchArtistImages(query: String) {
        if (query.isBlank()) {
            _candidateImages.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingImages.value = true
            val results = artistImageRepository.searchArtistCandidates(query)
            _candidateImages.value = results
            _isSearchingImages.value = false
        }
    }

    fun selectArtistImage(imageUrl: String) {
        val currentArtist = artist.value ?: return
        viewModelScope.launch {
            artistImageRepository.updateArtistImageUrl(currentArtist.id, currentArtist.name, imageUrl)
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            val activeController = controller ?: return@launch
            val currentSongs = songs.value
            if (currentSongs.isEmpty()) {
                activeController.setMediaItem(song.toMediaItem())
                activeController.prepare()
                activeController.play()
            } else {
                val startIndex = currentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                val songIds = ArrayList(currentSongs.map { it.id })
                val args = Bundle().apply {
                    putStringArrayList("song_ids", songIds)
                    putInt("start_index", startIndex)
                }
                activeController.sendCustomCommand(
                    SessionCommand("ACTION_PLAY_LIST", Bundle.EMPTY),
                    args
                )
            }
        }
    }

    fun playArtist(shuffle: Boolean = false) {
        viewModelScope.launch {
            val activeController = controller ?: return@launch
            val currentSongs = songs.value
            if (currentSongs.isEmpty()) return@launch

            activeController.shuffleModeEnabled = shuffle
            val startIndex = if (shuffle) currentSongs.indices.random() else 0
            val songIds = ArrayList(currentSongs.map { it.id })
            val args = Bundle().apply {
                putStringArrayList("song_ids", songIds)
                putInt("start_index", startIndex)
            }
            activeController.sendCustomCommand(
                SessionCommand("ACTION_PLAY_LIST", Bundle.EMPTY),
                args
            )
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
