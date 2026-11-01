package io.github.vinaygaykar.mp1.playback.session

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** Single source of truth for the custom protocol between app UI and [PlaybackService]. */
object PlaybackSessionContract {
    val PlayListCommand = SessionCommand("ACTION_PLAY_LIST", Bundle.EMPTY)
    val ToggleRepeatCommand = SessionCommand("ACTION_TOGGLE_REPEAT", Bundle.EMPTY)

    const val EXTRA_SONG_IDS = "song_ids"
    const val EXTRA_START_INDEX = "start_index"
    const val EXTRA_ALBUM_ID = "album_id"

    /** Unique id of one queue entry (the same song can be queued twice). Stamped by the service. */
    const val EXTRA_QUEUE_UID = "queue_uid"

    /** PlayList command: whether to switch shuffle on/off as part of starting the list. */
    const val EXTRA_SHUFFLE_MODE = "shuffle_mode"
    const val SHUFFLE_KEEP = -1
    const val SHUFFLE_OFF = 0
    const val SHUFFLE_ON = 1
}
