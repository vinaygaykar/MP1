package io.github.vinaygaykar.mp1.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaygaykar.mp1.MainActivity
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.data.local.dao.PlaybackStateDao
import io.github.vinaygaykar.mp1.data.local.dao.QueueDao
import io.github.vinaygaykar.mp1.data.local.entities.PlaybackStateEntity
import io.github.vinaygaykar.mp1.data.local.entities.QueueItemEntity
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import io.github.vinaygaykar.mp1.playback.session.PlaybackSessionContract
import io.github.vinaygaykar.mp1.playback.session.ensureQueueUid
import io.github.vinaygaykar.mp1.playback.session.queueUid
import io.github.vinaygaykar.mp1.playback.session.withQueueUid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRestoringState = true

    private var playListJob: Job? = null
    private var saveQueueJob: Job? = null

    /**
     * Shuffle is an app-level flag: the queue is always one concrete order, and switching shuffle
     * on reorders it once. This remembers each item's place in the order from before that
     * reshuffle (queue entry uid -> rank) so switching shuffle off can put it back.
     * Only filled while shuffle is on. Main thread only.
     */
    private val originalRanks = HashMap<String, Double>()

    /** True while the service itself flips shuffle, so the toggle handler doesn't reorder. */
    private var suppressShuffleReorder = false

    @Inject
    lateinit var playbackStateDao: PlaybackStateDao

    @Inject
    lateinit var queueDao: QueueDao

    @Inject
    lateinit var musicRepository: MusicRepository

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Keep ExoPlayer's own shuffle order as identity: the queue order is the play order,
        // whatever shuffleModeEnabled says.
        player.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))

        serviceScope.launch(Dispatchers.IO) {
            val savedState = playbackStateDao.getPlaybackState()
            val savedQueue = queueDao.getQueueSync()
            val songsById = musicRepository
                .getSongsByIdsSync(savedQueue.map { it.songId }.distinct())
                .associateBy { it.id }
            val restored = savedQueue.mapNotNull { entity ->
                songsById[entity.songId]?.let { entity to it.toMediaItem().withQueueUid(entity.queueItemId) }
            }

            launch(Dispatchers.Main) {
                val restoredItems = restored.map { it.second }
                if (isRestoringState && restoredItems.isNotEmpty()) {
                    val currentId = savedState?.currentQueueItemId
                    val startIndex = restoredItems.indexOfFirst { it.queueUid() == currentId }
                        .takeIf { it >= 0 }
                        // Older versions saved the song id instead of the queue entry uid.
                        ?: restoredItems.indexOfFirst { it.mediaId == currentId }.takeIf { it >= 0 }
                        ?: 0
                    val positionMs = savedState?.positionMs ?: 0L

                    restored.forEach { (entity, item) ->
                        entity.originalRank?.let { originalRanks[item.queueUid()!!] = it }
                    }
                    player.setMediaItems(restoredItems, startIndex, positionMs)
                    player.prepare()
                }

                if (isRestoringState && savedState != null) {
                    player.repeatMode = savedState.repeatMode
                    setShuffleFlag(player, savedState.isShuffleOn)
                    // Queue saved before original orders were stored: the order as it is now
                    // becomes the original one.
                    if (savedState.isShuffleOn && originalRanks.isEmpty()) snapshotOriginalOrder(player)
                }

                isRestoringState = false
                mediaSession?.setMediaButtonPreferences(buildMediaButtonPreferences(player))
            }
        }

        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                if (!isRestoringState) savePlaybackState(player)
                mediaSession?.setMediaButtonPreferences(buildMediaButtonPreferences(player))
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (!isRestoringState) savePlaybackState(player)
                // Same behaviour from every entry point (app, notification, Bluetooth, Auto).
                if (!isRestoringState && !suppressShuffleReorder) {
                    // Not inline: editing the playlist from inside a player callback makes the
                    // session see a timeline that no longer matches the player's live position.
                    // launch on Dispatchers.Main (not .immediate) runs it after this callback.
                    serviceScope.launch {
                        if (player.shuffleModeEnabled == shuffleModeEnabled) {
                            if (shuffleModeEnabled) shuffleQueue(player) else restoreOriginalOrder(player)
                        }
                    }
                }
                mediaSession?.setMediaButtonPreferences(buildMediaButtonPreferences(player))
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isRestoringState) savePlaybackState(player)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && !isRestoringState) {
                    if (player.shuffleModeEnabled) rankNewItems(player)
                    scheduleQueueAndPlaybackStateSave(player)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isRestoringState) savePlaybackState(player)
            }
        })

        val sessionIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(PlaybackSessionContract.ToggleRepeatCommand)
                    .add(PlaybackSessionContract.PlayListCommand)
                    .build()

                return MediaSession.ConnectionResult.accept(
                    availableSessionCommands,
                    connectionResult.availablePlayerCommands
                )
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand == PlaybackSessionContract.ToggleRepeatCommand) {
                    session.player.repeatMode = (session.player.repeatMode + 1) % 3
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                if (customCommand == PlaybackSessionContract.PlayListCommand) {
                    val songIds = args.getStringArrayList(PlaybackSessionContract.EXTRA_SONG_IDS) ?: emptyList()
                    val shuffleOn = when (args.getInt(PlaybackSessionContract.EXTRA_SHUFFLE_MODE, PlaybackSessionContract.SHUFFLE_KEEP)) {
                        PlaybackSessionContract.SHUFFLE_ON -> true
                        PlaybackSessionContract.SHUFFLE_OFF -> false
                        else -> session.player.shuffleModeEnabled
                    }
                    val requestedStart = args.getInt(PlaybackSessionContract.EXTRA_START_INDEX, -1)
                    val startIndex = when {
                        requestedStart in songIds.indices -> requestedStart
                        shuffleOn && songIds.isNotEmpty() -> songIds.indices.random()
                        else -> 0
                    }
                    playList(songIds, startIndex, shuffleOn)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                // Every entry added from a controller gets its own uid.
                return Futures.transform(
                    super.onAddMediaItems(mediaSession, controller, mediaItems),
                    Function<MutableList<MediaItem>, MutableList<MediaItem>> { items ->
                        items.map { it.ensureQueueUid() }.toMutableList()
                    },
                    MoreExecutors.directExecutor()
                )
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .build().also { session ->
                session.setMediaButtonPreferences(buildMediaButtonPreferences(player))
            }
    }

    private fun buildMediaButtonPreferences(player: Player): List<CommandButton> {
        val repeatIconRes = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all
            else -> R.drawable.ic_repeat_off
        }
        val repeatButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(repeatIconRes)
            .setDisplayName("Repeat")
            .setSessionCommand(PlaybackSessionContract.ToggleRepeatCommand)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

        return listOf(repeatButton)
    }

    /**
     * Replaces the queue with [songIds]. Shuffle off: the list as-is, playing [startIndex].
     * Shuffle on: the start song first and the rest shuffled once. Either way the original list
     * order is remembered when shuffling, so it can be restored.
     */
    private fun playList(songIds: List<String>, startIndex: Int, shuffleOn: Boolean) {
        if (songIds.isEmpty()) return
        playListJob?.cancel()
        isRestoringState = false

        val player = mediaSession?.player ?: return

        playListJob = serviceScope.launch(Dispatchers.IO) {
            val targetSong = musicRepository.getSongById(songIds[startIndex]) ?: return@launch
            val targetItem = targetSong.toMediaItem().withQueueUid()

            withContext(Dispatchers.Main) {
                setShuffleFlag(player, shuffleOn)
                originalRanks.clear()
                if (shuffleOn) originalRanks[targetItem.queueUid()!!] = startIndex.toDouble()
                player.setMediaItem(targetItem)
                player.prepare()
                player.play()
            }

            if (songIds.size <= 1) return@launch

            val byId = musicRepository.getSongsByIdsSync(songIds).associateBy { it.id }
            if (byId.isEmpty()) return@launch

            val ordered = songIds.mapNotNull(byId::get)
            val start = songIds.take(startIndex).count { it in byId }.coerceIn(0, ordered.lastIndex)
            val items = ordered.mapIndexed { i, song ->
                if (i == start) targetItem else song.toMediaItem().withQueueUid()
            }

            val itemsAfter: List<MediaItem>
            val itemsBefore: List<MediaItem>
            if (shuffleOn) {
                itemsAfter = items.filterIndexed { i, _ -> i != start }.shuffled()
                itemsBefore = emptyList()
            } else {
                itemsAfter = items.subList(start + 1, items.size)
                itemsBefore = items.subList(0, start)
            }

            delay(100)

            withContext(Dispatchers.Main) {
                // Ranks are in place before any item is added, so none is mistaken for a new one.
                originalRanks.clear()
                if (shuffleOn) {
                    items.forEachIndexed { i, item -> originalRanks[item.queueUid()!!] = i.toDouble() }
                }
            }

            val chunkSize = 150

            for (chunk in itemsAfter.chunked(chunkSize)) {
                withContext(Dispatchers.Main) {
                    player.addMediaItems(chunk)
                }
                delay(50)
            }

            var insertPos = 0
            for (chunk in itemsBefore.chunked(chunkSize)) {
                val currentPos = insertPos
                withContext(Dispatchers.Main) {
                    player.addMediaItems(currentPos, chunk)
                }
                insertPos += chunk.size
                delay(50)
            }
        }
    }

    private fun setShuffleFlag(player: Player, enabled: Boolean) {
        if (player.shuffleModeEnabled == enabled) return
        suppressShuffleReorder = true
        try {
            player.shuffleModeEnabled = enabled
        } finally {
            suppressShuffleReorder = false
        }
    }

    private fun currentItems(player: Player): List<MediaItem> {
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        return List(timeline.windowCount) { timeline.getWindow(it, window).mediaItem }
    }

    private fun snapshotOriginalOrder(player: Player) {
        originalRanks.clear()
        currentItems(player).forEachIndexed { i, item ->
            item.queueUid()?.let { originalRanks[it] = i.toDouble() }
        }
    }

    /** Shuffle switched on: remember the current order, then reshuffle all with the current song first. */
    private fun shuffleQueue(player: Player) {
        val items = currentItems(player)
        val current = player.currentMediaItemIndex
        snapshotOriginalOrder(player)
        if (items.size <= 1 || current !in items.indices) return

        val reordered = listOf(items[current]) +
            items.filterIndexed { i, _ -> i != current }.shuffled()
        applyOrder(player, reordered, currentPosition = 0)
    }

    /** Shuffle switched off: put the queue back in its remembered order; the current song keeps playing. */
    private fun restoreOriginalOrder(player: Player) {
        val items = currentItems(player)
        val current = player.currentMediaItemIndex
        // Stable sort: items without a rank keep their relative order at the end.
        val restored = items.sortedBy { item ->
            item.queueUid()?.let(originalRanks::get) ?: Double.MAX_VALUE
        }
        originalRanks.clear()
        if (items.size <= 1 || current !in items.indices) return

        applyOrder(player, restored, currentPosition = restored.indexOfFirst { it === items[current] })
    }

    /** Reorders the queue without interrupting the current song: only the items around it are replaced. */
    private fun applyOrder(player: Player, ordered: List<MediaItem>, currentPosition: Int) {
        val current = player.currentMediaItemIndex
        val count = player.mediaItemCount
        if (current !in 0 until count || currentPosition !in ordered.indices) return

        if (current + 1 < count) player.removeMediaItems(current + 1, count)
        if (current > 0) player.removeMediaItems(0, current)
        if (currentPosition > 0) player.addMediaItems(0, ordered.subList(0, currentPosition))
        if (currentPosition < ordered.lastIndex) {
            player.addMediaItems(ordered.subList(currentPosition + 1, ordered.size))
        }
    }

    /**
     * While shuffled, entries added from outside get a place in the original order:
     * at the end if appended, otherwise right after the entry before them (play next).
     */
    private fun rankNewItems(player: Player) {
        val uids = currentItems(player).map { it.queueUid() }
        uids.forEachIndexed { i, uid ->
            if (uid == null || uid in originalRanks) return@forEachIndexed
            val previous = if (i > 0) uids[i - 1]?.let(originalRanks::get) else null
            originalRanks[uid] = if (i == uids.lastIndex || previous == null) {
                (originalRanks.values.maxOrNull() ?: -1.0) + 1.0
            } else {
                val next = originalRanks.values.filter { it > previous }.minOrNull() ?: (previous + 1.0)
                (previous + next) / 2.0
            }
        }
    }

    private fun scheduleQueueAndPlaybackStateSave(player: Player) {
        if (isRestoringState) return
        saveQueueJob?.cancel()

        saveQueueJob = serviceScope.launch(Dispatchers.IO) {
            delay(500)

            val (queue, state) = withContext(Dispatchers.Main) {
                captureQueue(player) to capturePlaybackState(player)
            }

            queueDao.replaceQueue(queue)
            playbackStateDao.upsertPlaybackState(state)
        }
    }

    private fun savePlaybackState(player: Player) {
        if (isRestoringState) return

        val state = capturePlaybackState(player)

        serviceScope.launch(Dispatchers.IO) {
            playbackStateDao.upsertPlaybackState(state)
        }
    }

    private fun capturePlaybackState(player: Player) = PlaybackStateEntity(
        id = 0,
        currentQueueItemId = player.currentMediaItem?.let { it.queueUid() ?: it.mediaId },
        positionMs = player.currentPosition,
        isShuffleOn = player.shuffleModeEnabled,
        repeatMode = player.repeatMode
    )

    private fun captureQueue(player: Player): List<QueueItemEntity> {
        val shuffleOn = player.shuffleModeEnabled
        return currentItems(player).mapIndexed { i, item ->
            val uid = item.queueUid() ?: "$i-${item.mediaId}"
            QueueItemEntity(
                queueItemId = uid,
                position = i,
                songId = item.mediaId,
                sourceContext = null,
                originalRank = if (shuffleOn) originalRanks[uid] else null
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.player?.let { player ->
            if (!isRestoringState) {
                val state = capturePlaybackState(player)
                // serviceScope is cancelled below, so a launch here would never run.
                // Block briefly instead. Must not hop to Main inside (we are on Main).
                runBlocking {
                    withTimeoutOrNull(1_000) {
                        withContext(Dispatchers.IO) {
                            playbackStateDao.upsertPlaybackState(state)
                        }
                    }
                }
            }
        }
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}