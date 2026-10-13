package io.github.vinaygaykar.mp1.domain.models

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.vinaygaykar.mp1.utils.DEFAULT_ARTIST_DELIMITERS
import io.github.vinaygaykar.mp1.utils.DEFAULT_WORD_DELIMITERS
import io.github.vinaygaykar.mp1.utils.collectArtistNames

data class Song(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String? = null,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val artworkUri: Uri?,
    val fileName: String,
    val lastModified: Long,
    val artistsDisplayString: String = ""
)

/**
 * Convenience extension to extract all individual artist names from a [Song].
 */
fun Song.getArtists(
    delimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
    wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
    extractFromTitle: Boolean = true
): List<String> {
    if (delimiters == DEFAULT_ARTIST_DELIMITERS && wordDelimiters == DEFAULT_WORD_DELIMITERS && extractFromTitle && artistsDisplayString.isNotBlank()) {
        return artistsDisplayString.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return collectArtistNames(
        rawArtistName = artist,
        title = title,
        artistDelimiters = delimiters,
        wordDelimiters = wordDelimiters,
        extractFromTitle = extractFromTitle
    )
}

fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        if (albumId != null) {
            putString("album_id", albumId)
        }
    }

    val displayArtist = artistsDisplayString.ifBlank { artist.ifBlank { "Unknown Artist" } }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(displayArtist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setExtras(extras)
                .build()
        )
        .build()
}
