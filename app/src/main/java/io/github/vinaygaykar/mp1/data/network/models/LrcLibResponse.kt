package io.github.vinaygaykar.mp1.data.network.models

import com.google.gson.annotations.SerializedName

data class LrcLibResponse(
    val id: Long,
    val name: String?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val instrumental: Boolean?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)
