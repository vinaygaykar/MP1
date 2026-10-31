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
}
