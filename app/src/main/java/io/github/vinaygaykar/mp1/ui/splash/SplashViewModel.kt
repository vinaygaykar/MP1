package io.github.vinaygaykar.mp1.ui.splash

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.data.local.dao.PlaybackStateDao
import io.github.vinaygaykar.mp1.data.local.dao.QueueDao
import io.github.vinaygaykar.mp1.data.local.dao.ScanFolderDao
import io.github.vinaygaykar.mp1.data.local.dao.SongDao
import io.github.vinaygaykar.mp1.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val queueDao: QueueDao,
    private val playbackStateDao: PlaybackStateDao,
    private val scanFolderDao: ScanFolderDao,
    private val songDao: SongDao,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _albumArtUris = MutableStateFlow<List<Uri>>(emptyList())
    val albumArtUris: StateFlow<List<Uri>> = _albumArtUris.asStateFlow()

    /** null = not decided yet, false = no folders or no songs (native splash only), true = show animated splash. */
    private val _showSplash = MutableStateFlow<Boolean?>(null)
    val showSplash: StateFlow<Boolean?> = _showSplash.asStateFlow()

    private val hostReady = MutableStateFlow(false)

    /** True once the native splash can go away: splash decision made and MainScreen has its first data. */
    val isReadyToShow: Boolean get() = _showSplash.value != null && hostReady.value

    fun setHostReady() {
        hostReady.value = true
    }

    var hasSplashBeenCompleted: Boolean = false
        private set

    private companion object {
        const val MAX_SPLASH_ALBUM_ARTS = 50
    }

    fun markSplashCompleted() {
        hasSplashBeenCompleted = true
        _isLoading.value = false
    }

    init {
        decideSplash()
    }

    private fun decideSplash() {
        viewModelScope.launch(Dispatchers.IO) {
            val show = try {
                scanFolderDao.getAllFolders().first().isNotEmpty() && songDao.hasAnySongs()
            } catch (e: Exception) {
                false
            }
            if (show) {
                loadSplashAlbumArts()
            } else {
                markSplashCompleted() // nothing to animate: the native splash is enough
            }
            _showSplash.value = show
        }
    }

    private fun loadSplashAlbumArts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collectedUris = mutableListOf<Uri>()
                val seenUris = mutableSetOf<String>()

                // 1. Get queue items starting from current playing track
                val playbackState = playbackStateDao.getPlaybackState()
                val queueItems = queueDao.getQueueSync()

                if (queueItems.isNotEmpty()) {
                    val currentIndex = if (playbackState?.currentQueueItemId != null) {
                        queueItems.indexOfFirst { it.queueItemId == playbackState.currentQueueItemId }.coerceAtLeast(0)
                    } else 0

                    val orderedQueue = queueItems.drop(currentIndex) + queueItems.take(currentIndex)
                    val songIds = orderedQueue.map { it.songId }
                    val queueSongs = musicRepository.getSongsByIdsSync(songIds)
                    val songsById = queueSongs.associateBy { it.id }

                    for (songId in songIds) {
                        val song = songsById[songId]
                        val uri = song?.artworkUri
                        if (uri != null && seenUris.add(uri.toString())) {
                            collectedUris.add(uri)
                            if (collectedUris.size == MAX_SPLASH_ALBUM_ARTS) break
                        }
                    }
                }

                // 2. If fewer than MAX_SPLASH_ALBUM_ARTS valid album arts, pick random songs with valid artwork
                if (collectedUris.size < MAX_SPLASH_ALBUM_ARTS) {
                    val allSongs = musicRepository.getAllSongs().firstOrNull() ?: emptyList()
                    val candidateSongs = allSongs
                        .filter { it.artworkUri != null && !seenUris.contains(it.artworkUri.toString()) }
                        .shuffled()

                    for (song in candidateSongs) {
                        val uri = song.artworkUri ?: continue
                        if (seenUris.add(uri.toString())) {
                            collectedUris.add(uri)
                            if (collectedUris.size == MAX_SPLASH_ALBUM_ARTS) break
                        }
                    }
                }

                // 3. Apply threshold rules: If count < 5, clear list (icon only splash)
                val finalUris = if (collectedUris.size < 5) {
                    emptyList()
                } else {
                    collectedUris.take(MAX_SPLASH_ALBUM_ARTS)
                }

                // 4. Preload images in parallel into Coil's shared memory cache, at the size they are drawn
                if (finalUris.isNotEmpty()) {
                    val sizePx = (SPLASH_ALBUM_ART_SIZE.value * context.resources.displayMetrics.density).roundToInt()
                    coroutineScope {
                        finalUris.map { uri ->
                            async {
                                context.imageLoader.execute(
                                    ImageRequest.Builder(context).data(uri).size(sizePx).build()
                                )
                            }
                        }.awaitAll()
                    }
                }

                _albumArtUris.value = finalUris
            } catch (e: Exception) {
                _albumArtUris.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
