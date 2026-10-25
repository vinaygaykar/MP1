package io.github.vinaygaykar.mp1.ui.library.v2

import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import kotlin.math.roundToInt

enum class LibraryCategory(val title: String, val isListCategory: Boolean = true) {
    SEARCH("Search", isListCategory = false),
    PLAYLISTS("Playlists", isListCategory = true),
    SONGS("Songs", isListCategory = true),
    ALBUMS("Albums", isListCategory = true),
    ARTISTS("Artists", isListCategory = true)
}

fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

fun lerp(start: Int, stop: Int, fraction: Float): Int =
    (start + (stop - start) * fraction).roundToInt()

/**
 * Immutable state model containing domain data required for rendering the library.
 */
data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val useCleanAlbumGrid: Boolean = false,
    val isScanning: Boolean = false,
    val currentFolder: String? = null,
    val importError: String? = null
)
