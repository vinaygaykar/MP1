package io.github.vinaygaykar.mp1.ui.search

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.playback.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilterType(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var controller: MediaController? = null

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilters = MutableStateFlow<Set<SearchFilterType>>(emptySet())
    val selectedFilters = _selectedFilters.asStateFlow()

    val searchResults: StateFlow<SearchResults> = combine(
        _searchQuery.debounce(150),
        _selectedFilters
    ) { query, selectedFilters ->
        query to selectedFilters
    }.flatMapLatest { (query, selectedFilters) ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            flowOf(SearchResults())
        } else {
            combine(
                musicRepository.searchSongs(trimmed),
                musicRepository.searchAlbums(trimmed),
                musicRepository.searchArtists(trimmed)
            ) { songs, albums, artists ->
                val showSongs = selectedFilters.isEmpty() || selectedFilters.contains(SearchFilterType.SONGS)
                val showAlbums = selectedFilters.isEmpty() || selectedFilters.contains(SearchFilterType.ALBUMS)
                val showArtists = selectedFilters.isEmpty() || selectedFilters.contains(SearchFilterType.ARTISTS)

                SearchResults(
                    songs = if (showSongs) songs else emptyList(),
                    albums = if (showAlbums) albums else emptyList(),
                    artists = if (showArtists) artists else emptyList()
                )
            }
        }
    }
    .catch { emit(SearchResults()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilter(filter: SearchFilterType) {
        _selectedFilters.update { current ->
            if (current.contains(filter)) {
                current - filter
            } else {
                current + filter
            }
        }
    }

    fun onSongClick(song: Song) {
        val player = controller ?: return
        player.setMediaItem(song.toMediaItem())
        player.prepare()
        player.play()
    }

    fun addToQueue(song: Song) {
        val player = controller ?: return
        player.addMediaItem(song.toMediaItem())
    }

    override fun onCleared() {
        super.onCleared()
        controller?.release()
    }
}
