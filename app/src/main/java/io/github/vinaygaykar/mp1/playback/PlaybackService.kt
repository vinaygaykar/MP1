package io.github.vinaygaykar.mp1.playback

import android.app.PendingIntent
import android.content.Context
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
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaygaykar.mp1.MainActivity
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.data.local.dao.PlaybackStateDao
import io.github.vinaygaykar.mp1.data.local.dao.QueueDao
import io.github.vinaygaykar.mp1.data.local.entities.PlaybackStateEntity
import io.github.vinaygaykar.mp1.data.local.entities.QueueItemEntity
import io.github.vinaygaykar.mp1.domain.models.toMediaItem
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRestoringState = true

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

        serviceScope.launch(Dispatchers.IO) {
            val savedState = playbackStateDao.getPlaybackState()
            val savedSongs = musicRepository.getQueueSongsSync()

            val restoredMediaItems = savedSongs.map { it.toMediaItem() }

            launch(Dispatchers.Main) {
                if (restoredMediaItems.isNotEmpty()) {
                    val currentSongId = savedState?.currentQueueItemId
                    val startIndex = restoredMediaItems.indexOfFirst { it.mediaId == currentSongId }
                        .takeIf { it >= 0 } ?: 0
                    val positionMs = savedState?.positionMs ?: 0L

                    player.setMediaItems(restoredMediaItems, startIndex, positionMs)
                    player.prepare()
                }

                if (savedState != null) {
                    player.repeatMode = savedState.repeatMode
                    player.shuffleModeEnabled = savedState.isShuffleOn
                }

                isRestoringState = false
            }
        }

        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                if (!isRestoringState) savePlaybackState(player)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (!isRestoringState) savePlaybackState(player)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isRestoringState) savePlaybackState(player)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!isRestoringState) saveQueueAndPlaybackState(player)
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

        val stopCommand = SessionCommand("ACTION_STOP_PLAYBACK", Bundle.EMPTY)
        val stopButton = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(R.drawable.ic_close)
            .setSessionCommand(stopCommand)
            .build()

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(stopCommand)
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
                if (customCommand.customAction == "ACTION_STOP_PLAYBACK") {
                    session.player.stop()
                    session.player.clearMediaItems()
                    stopSelf()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .setCustomLayout(listOf(stopButton))
            .build()

        setMediaNotificationProvider(RightAlignedStopNotificationProvider(this))
    }

    private fun saveQueueAndPlaybackState(player: Player) {
        if (isRestoringState) return

        val currentTimeline = player.currentTimeline
        val count = currentTimeline.windowCount
        val window = Timeline.Window()
        val queueEntities = ArrayList<QueueItemEntity>(count)

        for (i in 0 until count) {
            currentTimeline.getWindow(i, window)
            val songId = window.mediaItem.mediaId
            queueEntities.add(
                QueueItemEntity(
                    queueItemId = "$i-$songId",
                    position = i,
                    songId = songId,
                    sourceContext = null
                )
            )
        }

        val currentSongId = player.currentMediaItem?.mediaId
        val currentPosition = player.currentPosition
        val repeatMode = player.repeatMode
        val isShuffleOn = player.shuffleModeEnabled

        serviceScope.launch(Dispatchers.IO) {
            queueDao.replaceQueue(queueEntities)
            playbackStateDao.upsertPlaybackState(
                PlaybackStateEntity(
                    id = 0,
                    currentQueueItemId = currentSongId,
                    positionMs = currentPosition,
                    isShuffleOn = isShuffleOn,
                    repeatMode = repeatMode
                )
            )
        }
    }

    private fun savePlaybackState(player: Player) {
        if (isRestoringState) return

        val currentSongId = player.currentMediaItem?.mediaId
        val currentPosition = player.currentPosition
        val repeatMode = player.repeatMode
        val isShuffleOn = player.shuffleModeEnabled

        serviceScope.launch(Dispatchers.IO) {
            playbackStateDao.upsertPlaybackState(
                PlaybackStateEntity(
                    id = 0,
                    currentQueueItemId = currentSongId,
                    positionMs = currentPosition,
                    isShuffleOn = isShuffleOn,
                    repeatMode = repeatMode
                )
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
                val currentSongId = player.currentMediaItem?.mediaId
                val currentPosition = player.currentPosition
                val repeatMode = player.repeatMode
                val isShuffleOn = player.shuffleModeEnabled
                serviceScope.launch(Dispatchers.IO) {
                    playbackStateDao.upsertPlaybackState(
                        PlaybackStateEntity(
                            id = 0,
                            currentQueueItemId = currentSongId,
                            positionMs = currentPosition,
                            isShuffleOn = isShuffleOn,
                            repeatMode = repeatMode
                        )
                    )
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

    @OptIn(UnstableApi::class)
    private class RightAlignedStopNotificationProvider(
        context: Context
    ) : MediaNotification.Provider {
        private val delegate = DefaultMediaNotificationProvider.Builder(context).build()

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            callback: MediaNotification.Provider.Callback
        ): MediaNotification {
            val mediaNotification = delegate.createNotification(
                mediaSession,
                customLayout,
                actionFactory,
                callback
            )
            val actions = mediaNotification.notification.actions
            if (actions != null && actions.size > 1) {
                val stopIndex = actions.indexOfFirst {
                    val title = it.title?.toString()
                    title.equals("Stop", ignoreCase = true) || title.equals("Close", ignoreCase = true)
                }
                if (stopIndex != -1 && stopIndex != actions.size - 1) {
                    val actionList = actions.toMutableList()
                    val stopAction = actionList.removeAt(stopIndex)
                    actionList.add(stopAction)
                    mediaNotification.notification.actions = actionList.toTypedArray()
                }
            }
            return mediaNotification
        }

        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean = delegate.handleCustomCommand(session, action, extras)
    }
}
