package io.github.vinaygaykar.mp1.domain.repository

import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.Song

interface LyricsRepository {
    suspend fun getLyrics(song: Song): Result<Lyrics>
    suspend fun searchLyrics(query: String, songId: String): Result<Lyrics>
}
