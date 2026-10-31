package io.github.vinaygaykar.mp1.playback.client

import android.os.Bundle
import androidx.media3.session.MediaController
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.playback.session.PlaybackSessionContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** What screens use to start playback and touch the queue. No screen talks to MediaController. */
@Singleton
class PlaybackClient @Inject constructor(
    private val connection: PlaybackConnection,
) {

    /**
     * Replaces the queue with [songs] and starts playing.
     * @param startAt song to start from (defaults to the first one)
     * @param shuffle when non-null, sets shuffle mode and (if true) starts from a random song;
     *                null leaves the current shuffle mode untouched
     */
    suspend fun play(songs: List<Song>, startAt: Song? = null, shuffle: Boolean? = null) =
        withContext(Dispatchers.Main.immediate) {
            val controller = connection.controller()
            if (songs.isEmpty()) {
                startAt?.let { controller.playSingle(it) }
                return@withContext
            }
            shuffle?.let { controller.shuffleModeEnabled = it }
            val start = when {
                shuffle == true -> songs.indices.random()
                else -> songs.indexOfFirst { it.id == startAt?.id }.coerceAtLeast(0)
            }
            controller.sendCustomCommand(
                PlaybackSessionContract.PlayListCommand,
                Bundle().apply {
                    putStringArrayList(
                        PlaybackSessionContract.EXTRA_SONG_IDS,
                        ArrayList(songs.map { it.id }),
                    )
                    putInt(PlaybackSessionContract.EXTRA_START_INDEX, start)
                },
            ).await()
        }

    /** Replaces the queue with just [song]. */
    suspend fun playSingle(song: Song) = withContext(Dispatchers.Main.immediate) {
        connection.controller().playSingle(song)
    }

    suspend fun addToQueue(song: Song) = withContext(Dispatchers.Main.immediate) {
        connection.controller().addMediaItem(song.toMediaItem())
    }

    private fun MediaController.playSingle(song: Song) {
        setMediaItem(song.toMediaItem())
        prepare()
        play()
    }
}
