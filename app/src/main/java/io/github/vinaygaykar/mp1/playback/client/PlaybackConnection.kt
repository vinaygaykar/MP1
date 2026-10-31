package io.github.vinaygaykar.mp1.playback.client

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one [MediaController] for the whole app. Never release it from a ViewModel;
 * it lives as long as the process.
 *
 * If the session service dies, the controller is dropped and one reconnect is attempted.
 * Observers of [current] get null, then the new controller, and must rebind to it.
 * [controller] always returns a live controller, reconnecting on demand.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _current = MutableStateFlow<MediaController?>(null)
    val current: StateFlow<MediaController?> = _current.asStateFlow()

    private val disconnectListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (_current.value === controller) _current.value = null
            controller.release()
            scope.launch {
                delay(RECONNECT_DELAY_MS)
                runCatching { this@PlaybackConnection.controller() }
            }
        }
    }

    init {
        scope.launch { runCatching { controller() } }
    }

    suspend fun controller(): MediaController = withContext(Dispatchers.Main.immediate) {
        mutex.withLock {
            _current.value ?: connect().also { _current.value = it }
        }
    }

    private suspend fun connect(): MediaController = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).setListener(disconnectListener).buildAsync().await()

    private companion object {
        const val RECONNECT_DELAY_MS = 1_000L
    }
}
