package io.github.vinaygaykar.mp1.ui.faces

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState

interface PlayerFace {
    val id: String
    val displayName: String

    @Composable
    fun Render(
        state: PlayerUiState,
        actions: PlayerActions,
        modifier: Modifier,
        showRecord: Boolean,
        onArtistClick: (() -> Unit)?
    )

    @Composable
    fun Render(
        state: PlayerUiState,
        actions: PlayerActions,
        modifier: Modifier,
        showRecord: Boolean
    ) {
        Render(
            state = state,
            actions = actions,
            modifier = modifier,
            showRecord = showRecord,
            onArtistClick = null
        )
    }
}

object PlayerFaceRegistry {
    val faces = mutableMapOf<String, PlayerFace>()

    fun register(face: PlayerFace) {
        faces[face.id] = face
    }

    fun getFace(id: String): PlayerFace? = faces[id]
}
