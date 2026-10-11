package io.github.vinaygaykar.mp1.domain.models

import android.net.Uri

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val year: Int?,
    val artworkUri: Uri?,
    val trackCount: Int
)
