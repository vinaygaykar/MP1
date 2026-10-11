package io.github.vinaygaykar.mp1.domain.models

data class Lyrics(
    val content: String,
    val isSynced: Boolean,
    val lines: List<LyricsLine> = emptyList()
)

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)
