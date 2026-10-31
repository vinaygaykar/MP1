package io.github.vinaygaykar.mp1.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaygaykar.mp1.data.network.models.DeezerArtist
import io.github.vinaygaykar.mp1.data.repo.ArtistImageRepository
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.client.PlaybackClient
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistImageRepository: ArtistImageRepository,
    savedStateHandle: SavedStateHandle,
    private val playback: PlaybackClient
) : ViewModel() {

    val artistId: String = savedStateHandle.get<String>(ArtistDetailActivity.EXTRA_ARTIST_ID) ?: ""

    private val _candidateImages = MutableStateFlow<List<DeezerArtist>>(emptyList())
    val candidateImages: StateFlow<List<DeezerArtist>> = _candidateImages.asStateFlow()

    private val _isSearchingImages = MutableStateFlow(false)
    val isSearchingImages: StateFlow<Boolean> = _isSearchingImages.asStateFlow()

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
        viewModelScope.launch { playback.play(songs.value, startAt = song) }
    }

    fun playArtist(shuffle: Boolean = false) {
        viewModelScope.launch { playback.play(songs.value, shuffle = shuffle) }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch { playback.addToQueue(song) }
    }
}
