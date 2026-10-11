package io.github.vinaygaykar.mp1.ui.library

import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.LyricsLine

object LyricsParser {
    private val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")

    fun parse(content: String, isSynced: Boolean): Lyrics {
        if (!isSynced) {
            return Lyrics(content = content, isSynced = false)
        }

        val lines = mutableListOf<LyricsLine>()
        content.lines().forEach { line ->
            val match = lrcRegex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val milliseconds = match.groupValues[3].padEnd(3, '0').toLong()
                val timestamp = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                val text = match.groupValues[4].trim()
                lines.add(LyricsLine(timestamp, text))
            }
        }

        return if (lines.isEmpty()) {
            Lyrics(content = content, isSynced = false)
        } else {
            Lyrics(content = content, isSynced = true, lines = lines.sortedBy { it.timestampMs })
        }
    }
}
