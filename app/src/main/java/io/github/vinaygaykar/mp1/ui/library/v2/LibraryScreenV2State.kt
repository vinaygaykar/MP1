package io.github.vinaygaykar.mp1.ui.library.v2

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.library.DragValue

/**
 * State holder managing transient interaction state, navigation category,
 * selections, sheet toggles, and swipe gestures.
 */
class LibraryState(
    val anchoredDraggableState: AnchoredDraggableState<DragValue>,
    initialCategory: LibraryCategory = LibraryCategory.SONGS
) {
    var lastOpenedCategory by mutableStateOf(initialCategory)
    var selectedSongIds by mutableStateOf(setOf<String>())
    var selectedPlaylistIds by mutableStateOf(setOf<Long>())

    var showCreateDialog by mutableStateOf(false)
    var showPlaylistSelectionSheet by mutableStateOf(false)
    var showCreatePlaylistForSelectionDialog by mutableStateOf(false)
    var showMultiSelectionSheet by mutableStateOf(false)
    var showPlaylistMultiSelectionSheet by mutableStateOf(false)

    val isSelectionMode: Boolean get() = selectedSongIds.isNotEmpty()
    val isPlaylistSelectionMode: Boolean get() = selectedPlaylistIds.isNotEmpty()

    suspend fun animateToExpanded() {
        anchoredDraggableState.animateTo(DragValue.EXPANDED)
    }

    suspend fun animateToCollapsed() {
        anchoredDraggableState.animateTo(DragValue.COLLAPSED)
    }

    fun toggleSongSelection(song: Song) {
        selectedSongIds = if (selectedSongIds.contains(song.id)) {
            selectedSongIds - song.id
        } else {
            selectedSongIds + song.id
        }
    }

    fun togglePlaylistSelection(playlist: Playlist) {
        selectedPlaylistIds = if (selectedPlaylistIds.contains(playlist.id)) {
            selectedPlaylistIds - playlist.id
        } else {
            selectedPlaylistIds + playlist.id
        }
    }

    fun clearSongSelection() {
        selectedSongIds = emptySet()
    }

    fun clearPlaylistSelection() {
        selectedPlaylistIds = emptySet()
    }

    fun clearAllSelections() {
        clearSongSelection()
        clearPlaylistSelection()
    }

    fun selectAllSongs(songs: List<Song>) {
        selectedSongIds = songs.map { it.id }.toSet()
    }

    fun selectAllPlaylists(playlists: List<Playlist>) {
        selectedPlaylistIds = playlists.map { it.id }.toSet()
    }
}
