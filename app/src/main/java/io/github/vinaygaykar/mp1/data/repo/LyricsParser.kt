package io.github.vinaygaykar.mp1.data.repo

import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.LyricsLine

fun LyricsLine.isInstrumental(): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed in listOf("♪", "♫", "♬", "🎶", "🎵", "...", "---", "♪♪♪", "♪ ♪ ♪", "♪♪", "♪ ♪")) return true
    val lower = trimmed.lowercase()
    return (lower == "(instrumental)" || lower == "[instrumental]" || lower == "instrumental" ||
           lower == "(music)" || lower == "[music]" || lower == "music")
}

object LyricsParser {
    private val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")

    fun parse(content: String, isSynced: Boolean): Lyrics {
        if (!isSynced) {
            return Lyrics(content = content, isSynced = false)
        }

        val rawLines = mutableListOf<LyricsLine>()
        content.lines().forEach { line ->
            val match = lrcRegex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val milliseconds = match.groupValues[3].padEnd(3, '0').toLong()
                val timestamp = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                val text = match.groupValues[4].trim()
                rawLines.add(LyricsLine(timestamp, text))
            }
        }

        if (rawLines.isEmpty()) {
            return Lyrics(content = content, isSynced = false)
        }

        val sorted = rawLines.sortedBy { it.timestampMs }
        val processedLines = processInstrumentalGaps(sorted)

        return Lyrics(content = content, isSynced = true, lines = processedLines)
    }

    private fun processInstrumentalGaps(sortedLines: List<LyricsLine>): List<LyricsLine> {
        if (sortedLines.isEmpty()) return emptyList()

        val result = mutableListOf<LyricsLine>()

        // 1. Check intro instrumental break before the first lyric line
        val first = sortedLines.first()
        if (first.timestampMs >= 5000L && !first.isInstrumental()) {
            result.add(LyricsLine(0L, "♪"))
        }

        // 2. Process gaps between consecutive lines
        for (i in sortedLines.indices) {
            val current = sortedLines[i]
            result.add(current)

            if (i < sortedLines.size - 1) {
                val next = sortedLines[i + 1]
                if (!current.isInstrumental() && !next.isInstrumental()) {
                    val charCount = current.text.length
                    val estimatedDurationMs = (charCount * 120L + 2000L).coerceIn(3000L, 6000L)
                    val gapStartMs = current.timestampMs + estimatedDurationMs

                    // If there is an empty gap of at least 4 seconds before the next line starts
                    if (next.timestampMs - gapStartMs >= 4000L) {
                        result.add(LyricsLine(gapStartMs, "♪"))
                    }
                }
            }
        }

        return result
    }
}
