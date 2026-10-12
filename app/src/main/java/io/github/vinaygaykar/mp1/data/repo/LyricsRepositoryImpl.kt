package io.github.vinaygaykar.mp1.data.repo

import io.github.vinaygaykar.mp1.data.network.LrcLibService
import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.repository.LyricsRepository
import io.github.vinaygaykar.mp1.ui.library.LyricsParser
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    private val lrcLibService: LrcLibService
) : LyricsRepository {

    override suspend fun getLyrics(song: Song): Result<Lyrics> {
        // 1. Check local cache (passed via Song model)
        if (!song.lyrics.isNullOrBlank()) {
            return Result.success(LyricsParser.parse(song.lyrics, song.isLyricsSynced))
        }

        // 2. Fetch from LRCLIB
        return try {
            val duration = if (song.durationMs > 0) (song.durationMs / 1000).toInt() else null
            val response = lrcLibService.getLyrics(
                artistName = song.artist,
                trackName = song.title,
                albumName = song.album.takeIf { it.isNotBlank() && it != "Unknown Album" },
                duration = duration
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val content = body.syncedLyrics ?: body.plainLyrics
                    if (content != null) {
                        val isSynced = body.syncedLyrics != null
                        Result.success(LyricsParser.parse(content, isSynced))
                    } else if (body.instrumental == true) {
                        val instrumentalText = "[Instrumental]"
                        Result.success(Lyrics(content = instrumentalText, isSynced = false))
                    } else {
                        Result.failure(Exception("Lyrics not found"))
                    }
                } else {
                    Result.failure(Exception("Lyrics not found"))
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchLyrics(query: String, songId: String): Result<Lyrics> {
        return try {
            val response = lrcLibService.searchLyrics(query)
            if (response.isSuccessful) {
                val bodyList = response.body()
                if (!bodyList.isNullOrEmpty()) {
                    val body = bodyList.first()
                    val content = body.syncedLyrics ?: body.plainLyrics
                    if (content != null) {
                        val isSynced = body.syncedLyrics != null
                        Result.success(LyricsParser.parse(content, isSynced))
                    } else if (body.instrumental == true) {
                        val instrumentalText = "[Instrumental]"
                        Result.success(Lyrics(content = instrumentalText, isSynced = false))
                    } else {
                        Result.failure(Exception("Lyrics not found in first result"))
                    }
                } else {
                    Result.failure(Exception("No results found for \"$query\""))
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
