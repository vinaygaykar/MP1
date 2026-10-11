package io.github.vinaygaykar.mp1.domain.models

data class Artist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val imageUrl: String? = null
)
