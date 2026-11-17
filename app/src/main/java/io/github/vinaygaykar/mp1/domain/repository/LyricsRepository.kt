package io.github.vinaygaykar.mp1.domain.repository

import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.Song

class LyricsNotFoundException : Exception("No lyrics found")
class LyricsNetworkException : Exception("No internet connection or offline lyrics")
class LyricsHttpException(val statusCode: Int) : Exception("Failed to fetch lyrics")

interface LyricsRepository {
    suspend fun getLyrics(song: Song): Result<Lyrics>
    suspend fun searchLyrics(query: String, songId: String): Result<Lyrics>
    suspend fun isLocalOrEmbeddedLyrics(song: Song): Boolean
    suspend fun saveLyrics(song: Song, lyricsContent: String): Boolean
}
