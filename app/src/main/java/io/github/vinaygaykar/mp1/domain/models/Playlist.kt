package io.github.vinaygaykar.mp1.domain.models

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
