package io.github.vinaygaykar.mp1.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {
        // Force migrate all users to ENERGY playback wave type regardless of previous selection
        if (prefs.getString(KEY_PLAYBACK_WAVE, null) != PlaybackWaveType.ENERGY.name) {
            prefs.edit { putString(KEY_PLAYBACK_WAVE, PlaybackWaveType.ENERGY.name) }
        }
    }

    private val _useCleanAlbumGrid = MutableStateFlow(
        prefs.getBoolean(KEY_USE_CLEAN_ALBUM_GRID, false),
    )
    val useCleanAlbumGrid: StateFlow<Boolean> = _useCleanAlbumGrid.asStateFlow()

    fun setUseCleanAlbumGrid(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_USE_CLEAN_ALBUM_GRID, enabled) }
        _useCleanAlbumGrid.value = enabled
    }

    private val _playbackWave = MutableStateFlow(
        PlaybackWaveType.fromName(prefs.getString(KEY_PLAYBACK_WAVE, PlaybackWaveType.ENERGY.name)),
    )
    val playbackWave: StateFlow<PlaybackWaveType> = _playbackWave.asStateFlow()

    fun setPlaybackWave(value: PlaybackWaveType) {
        prefs.edit { putString(KEY_PLAYBACK_WAVE, value.name) }
        _playbackWave.value = value
    }

    companion object {
        private const val KEY_USE_CLEAN_ALBUM_GRID = "use_clean_album_grid"
        private const val KEY_PLAYBACK_WAVE = "playback_wave"
    }
}
