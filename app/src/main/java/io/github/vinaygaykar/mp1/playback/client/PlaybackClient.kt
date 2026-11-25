package io.github.vinaygaykar.mp1.playback.client

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
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
     * @param startAt song to start from (first song, or random if shuffling, when null)
     * @param shuffle true/false switches shuffle on/off for this list; null keeps the current setting
     */
    suspend fun play(songs: List<Song>, startAt: Song? = null, shuffle: Boolean? = null) =
        withContext(Dispatchers.Main.immediate) {
            val controller = connection.controller()
            if (songs.isEmpty()) {
                startAt?.let { controller.playSingle(it) }
                return@withContext
            }
            // The service owns queue order and shuffle: it queues the list, applies the shuffle
            // setting and picks the start song (random when shuffling with no start song).
            controller.sendCustomCommand(
                PlaybackSessionContract.PlayListCommand,
                Bundle().apply {
                    putStringArrayList(
                        PlaybackSessionContract.EXTRA_SONG_IDS,
                        ArrayList(songs.map { it.id }),
                    )
                    putInt(
                        PlaybackSessionContract.EXTRA_START_INDEX,
                        songs.indexOfFirst { it.id == startAt?.id },
                    )
                    putInt(
                        PlaybackSessionContract.EXTRA_SHUFFLE_MODE,
                        when (shuffle) {
                            null -> PlaybackSessionContract.SHUFFLE_KEEP
                            true -> PlaybackSessionContract.SHUFFLE_ON
                            false -> PlaybackSessionContract.SHUFFLE_OFF
                        },
                    )
                },
            ).await()
        }

    /** Replaces the queue with just [song]. */
    suspend fun playSingle(song: Song) = withContext(Dispatchers.Main.immediate) {
        connection.controller().playSingle(song)
    }

    /** Adds [song] to the end of the queue. Starts playing if the queue is empty. */
    suspend fun addToQueue(song: Song) = addToQueue(listOf(song))

    /** Adds [songs], in the given order, to the end of the queue. Starts playing if the queue is empty. */
    suspend fun addToQueue(songs: List<Song>) =
        sendQueueCommand(PlaybackSessionContract.AddToQueueCommand, songs)

    /**
     * Adds [songs], in the given order, right after the current song. Songs already added with
     * an earlier play-next end up after these. Starts playing if the queue is empty.
     */
    suspend fun playNext(songs: List<Song>) =
        sendQueueCommand(PlaybackSessionContract.PlayNextCommand, songs)

    // The service owns the queue, so it can hold these back while a long list is still loading.
    private suspend fun sendQueueCommand(command: SessionCommand, songs: List<Song>) {
        if (songs.isEmpty()) return
        withContext(Dispatchers.Main.immediate) {
            connection.controller().sendCustomCommand(
                command,
                Bundle().apply {
                    putStringArrayList(
                        PlaybackSessionContract.EXTRA_SONG_IDS,
                        ArrayList(songs.map { it.id }),
                    )
                },
            ).await()
        }
    }

    private fun MediaController.playSingle(song: Song) {
        setMediaItem(song.toMediaItem())
        prepare()
        play()
    }
}
