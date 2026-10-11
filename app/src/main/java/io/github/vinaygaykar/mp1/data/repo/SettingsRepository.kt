package io.github.vinaygaykar.mp1.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
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

    private val _useCleanAlbumGrid = MutableStateFlow(
        prefs.getBoolean(KEY_USE_CLEAN_ALBUM_GRID, false),
    )
    val useCleanAlbumGrid: StateFlow<Boolean> = _useCleanAlbumGrid.asStateFlow()

    fun setUseCleanAlbumGrid(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_USE_CLEAN_ALBUM_GRID, enabled) }
        _useCleanAlbumGrid.value = enabled
    }

    companion object {
        private const val KEY_USE_CLEAN_ALBUM_GRID = "use_clean_album_grid"
    }
}
