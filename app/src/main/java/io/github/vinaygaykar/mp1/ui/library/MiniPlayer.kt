package io.github.vinaygaykar.mp1.ui.library

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    actions: PlayerActions,
    showRecord: Boolean = true,
    rotation: Float? = null,
    textAlpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    onPositioned: ((Offset) -> Unit)? = null
) {
    if (state.trackTitle.isEmpty()) return

    val view = LocalView.current

    val internalRotation = if (rotation == null) {
        val targetRotation = state.positionMs * 0.12f
        val animRotation by animateFloatAsState(
            targetValue = targetRotation,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing),
            label = "mini_vinyl_rotation"
        )
        animRotation
    } else rotation

    var dragAmount by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .draggable(
                state = rememberDraggableState { delta ->
                    dragAmount += delta
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (dragAmount > 300f) {
                        if (state.hasPrevious) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            actions.onPrevious()
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    } else if (dragAmount < -300f) {
                        if (state.hasNext) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            actions.onNext()
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                    dragAmount = 0f
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp)
                        .graphicsLayer { alpha = textAlpha },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    val displayArtist = remember(state.artist, state.trackTitle) {
                        collectArtistNames(state.artist, state.trackTitle).joinArtistsForDisplay()
                    }

                    Text(
                        text = state.trackTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vinyl Record on the right side
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .alpha(if (showRecord) 1f else 0f)
                        .graphicsLayer { rotationZ = internalRotation % 360f }
                        .clip(CircleShape)
                        .onGloballyPositioned { coords ->
                            onPositioned?.invoke(coords.positionInRoot())
                        }
                ) {
                    AsyncImage(
                        model = state.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Bottom Progress Bar
            val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .align(Alignment.BottomCenter)
                    .height(2.dp)
                    .graphicsLayer { alpha = textAlpha },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
