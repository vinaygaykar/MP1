package io.github.vinaygaykar.mp1.ui.album

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.getArtists
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.domain.repository.PlaylistRepository
import io.github.vinaygaykar.mp1.playback.PlaybackService
import io.github.vinaygaykar.mp1.utils.splitArtistsByDelimiters
import io.github.vinaygaykar.mp1.utils.toArtistId
import io.github.vinaygaykar.mp1.utils.traceSection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val albumId: String = savedStateHandle.get<String>(AlbumDetailActivity.EXTRA_ALBUM_ID) ?: ""

    private var controller: MediaController? = null

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
        }
    }

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

    fun playAlbum(shuffle: Boolean = false) {
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
