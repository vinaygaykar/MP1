package io.github.vinaygaykar.mp1.playback.session

import android.os.Bundle
import androidx.media3.common.MediaItem
import java.util.UUID

/** Unique id of this queue entry, or null for an item that was never stamped. */
fun MediaItem.queueUid(): String? =
    mediaMetadata.extras?.getString(PlaybackSessionContract.EXTRA_QUEUE_UID)

fun MediaItem.withQueueUid(uid: String = UUID.randomUUID().toString()): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        putString(PlaybackSessionContract.EXTRA_QUEUE_UID, uid)
    }
    return buildUpon()
        .setMediaMetadata(mediaMetadata.buildUpon().setExtras(extras).build())
        .build()
}

fun MediaItem.ensureQueueUid(): MediaItem = if (queueUid() != null) this else withQueueUid()
