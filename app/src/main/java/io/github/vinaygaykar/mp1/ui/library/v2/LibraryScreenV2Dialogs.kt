package io.github.vinaygaykar.mp1.ui.library.v2

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.library.MultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistMultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet

/**
 * Encapsulates dialogs and bottom sheets triggered within the Library screen.
 */
@Composable
internal fun LibraryDialogsAndSheets(
    uiState: LibraryUiState,
    libraryState: LibraryState,
    onDismissImportError: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylists: (Set<Long>) -> Unit,
    onAddSongsToPlaylist: (Long, List<Song>) -> Unit,
    onCreatePlaylistWithSongs: (String, List<Song>) -> Unit
) {
    val selectedSongs = remember(uiState.songs, libraryState.selectedSongIds) {
        uiState.songs.filter { libraryState.selectedSongIds.contains(it.id) }
    }
    val selectedPlaylists = remember(uiState.playlists, libraryState.selectedPlaylistIds) {
        uiState.playlists.filter { libraryState.selectedPlaylistIds.contains(it.id) }
    }

    if (libraryState.showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { libraryState.showCreateDialog = false },
            onCreate = { name -> onCreatePlaylist(name) }
        )
    }

    if (uiState.importError != null) {
        AlertDialog(
            onDismissRequest = onDismissImportError,
            title = {
                Text(
                    text = "Import Failed",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = uiState.importError,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissImportError) {
                    Text("OK")
                }
            }
        )
    }

    if (libraryState.showCreatePlaylistForSelectionDialog) {
        CreatePlaylistDialog(
            onDismiss = { libraryState.showCreatePlaylistForSelectionDialog = false },
            onCreate = { name ->
                onCreatePlaylistWithSongs(name, selectedSongs)
                libraryState.clearSongSelection()
            }
        )
    }

    if (libraryState.showMultiSelectionSheet) {
        MultiSelectionBottomSheet(
            selectedSongs = selectedSongs,
            onDismiss = { libraryState.showMultiSelectionSheet = false },
            onPlayAll = {
                if (selectedSongs.isNotEmpty()) {
                    libraryState.clearSongSelection()
                }
            },
            onAddToPlaylist = {
                libraryState.showMultiSelectionSheet = false
                libraryState.showPlaylistSelectionSheet = true
            },
            onAddToQueue = {
                libraryState.clearSongSelection()
            }
        )
    }

    if (libraryState.showPlaylistSelectionSheet) {
        PlaylistPickerBottomSheet(
            playlists = uiState.playlists,
            onDismiss = { libraryState.showPlaylistSelectionSheet = false },
            onSelectPlaylist = { playlist ->
                onAddSongsToPlaylist(playlist.id, selectedSongs)
                libraryState.clearSongSelection()
            },
            onCreateNewPlaylist = {
                libraryState.showPlaylistSelectionSheet = false
                libraryState.showCreatePlaylistForSelectionDialog = true
            }
        )
    }

    if (libraryState.showPlaylistMultiSelectionSheet) {
        PlaylistMultiSelectionBottomSheet(
            selectedPlaylists = selectedPlaylists,
            onDismiss = { libraryState.showPlaylistMultiSelectionSheet = false },
            onDeletePlaylists = {
                onDeletePlaylists(libraryState.selectedPlaylistIds)
                libraryState.clearPlaylistSelection()
            }
        )
    }
}
