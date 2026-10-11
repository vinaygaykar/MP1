package io.github.vinaygaykar.mp1.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
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

        mediaSession = MediaSession.Builder(this, player).build()
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
}
