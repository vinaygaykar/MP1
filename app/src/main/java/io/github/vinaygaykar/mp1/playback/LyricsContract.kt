package io.github.vinaygaykar.mp1.playback

import androidx.compose.runtime.Immutable

sealed interface LyricsUiState {
    object None : LyricsUiState
    object Loading : LyricsUiState

    @Immutable
    data class Success(
        val lyrics: String,
        val isSynced: Boolean = false,
        val isLocal: Boolean = false,
    ) : LyricsUiState

    @Immutable
    data class Error(val message: String) : LyricsUiState
}

interface LyricsControls {
    fun onLyricsSearch(query: String)
    fun onSaveLyrics() {}
}
