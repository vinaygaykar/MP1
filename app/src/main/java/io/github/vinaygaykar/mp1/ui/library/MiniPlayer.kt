package io.github.vinaygaykar.mp1.ui.library

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    actions: PlayerActions,
    showRecord: Boolean = true,
    rotation: Float? = null,
    textAlpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    onPositioned: ((Offset) -> Unit)? = null,
) {
    if (state.trackTitle.isEmpty()) return

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val internalRotation = if (rotation == null) {
        val targetRotation = state.positionMs * 0.12f
        val animRotation by animateFloatAsState(
            targetValue = targetRotation,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing),
            label = "mini_vinyl_rotation"
        )
        animRotation
    } else rotation

    val prevItem = remember(state.queue, state.currentIndex, state.hasPrevious) {
        if (!state.hasPrevious || state.queue.isEmpty()) null
        else {
            val prevIndex = if (state.currentIndex > 0) {
                state.currentIndex - 1
            } else if (state.queue.isNotEmpty()) {
                state.queue.size - 1
            } else -1
            if (prevIndex in state.queue.indices) state.queue[prevIndex] else null
        }
    }

    val nextItem = remember(state.queue, state.currentIndex, state.hasNext) {
        if (!state.hasNext || state.queue.isEmpty()) null
        else {
            val nextIndex = if ((state.currentIndex in state.queue.indices) && (state.currentIndex + 1 < state.queue.size)) {
                state.currentIndex + 1
            } else if (state.queue.isNotEmpty()) {
                0
            } else -1
            if (nextIndex in state.queue.indices) state.queue[nextIndex] else null
        }
    }

    val nextTitle = remember(nextItem) { nextItem?.mediaMetadata?.title?.toString() ?: "" }
    val nextArtist = remember(nextItem) { nextItem?.mediaMetadata?.artist?.toString() ?: "" }
    val nextArtworkUri = remember(nextItem) { nextItem?.mediaMetadata?.artworkUri }

    val prevTitle = remember(prevItem) { prevItem?.mediaMetadata?.title?.toString() ?: "" }
    val prevArtist = remember(prevItem) { prevItem?.mediaMetadata?.artist?.toString() ?: "" }
    val prevArtworkUri = remember(prevItem) { prevItem?.mediaMetadata?.artworkUri }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    val animatableOffset = remember { Animatable(0f) }

    LaunchedEffect(state.trackTitle, state.currentIndex) {
        animatableOffset.snapTo(0f)
        rawDragOffset = 0f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .draggable(
                state = rememberDraggableState { delta ->
                    rawDragOffset += delta
                    val effective = calculateEffectiveOffset(
                        rawOffset = rawDragOffset,
                        width = containerWidthPx,
                        hasPrevious = state.hasPrevious,
                        hasNext = state.hasNext
                    )
                    coroutineScope.launch {
                        animatableOffset.snapTo(effective)
                    }
                },
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    rawDragOffset = animatableOffset.value
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        val currentOffset = animatableOffset.value
                        val width = if (containerWidthPx > 0f) containerWidthPx else 400f
                        val threshold = width * 0.25f
                        val velocityThreshold = 500f

                        if (currentOffset < 0f) { // Swiping left (Next)
                            if (state.hasNext && (abs(currentOffset) > threshold || velocity < -velocityThreshold)) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                animatableOffset.animateTo(
                                    targetValue = -width,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                actions.onNext()
                                delay(500)
                                if (animatableOffset.value != 0f) {
                                    animatableOffset.snapTo(0f)
                                    rawDragOffset = 0f
                                }
                            } else {
                                if (!state.hasNext && abs(currentOffset) > 15f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                animatableOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                                rawDragOffset = 0f
                            }
                        } else if (currentOffset > 0f) { // Swiping right (Prev)
                            if (state.hasPrevious && (currentOffset > threshold || velocity > velocityThreshold)) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                animatableOffset.animateTo(
                                    targetValue = width,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                                actions.onPrevious()
                                delay(500)
                                if (animatableOffset.value != 0f) {
                                    animatableOffset.snapTo(0f)
                                    rawDragOffset = 0f
                                }
                            } else {
                                if (!state.hasPrevious && currentOffset > 15f) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                animatableOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                                rawDragOffset = 0f
                            }
                        }
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
                .clipToBounds()
        ) {
            val offset = animatableOffset.value
            val width = containerWidthPx

            // Calculate scale and transform origin for stretch effect when no prev/next item
            val isStretchingNext = offset < 0f && !state.hasNext
            val isStretchingPrev = offset > 0f && !state.hasPrevious

            val stretchFactor = if (isStretchingNext || isStretchingPrev) {
                if (width > 0f) (abs(offset) / width).coerceIn(0f, 0.4f) else 0f
            } else 0f

            val currentScaleX = 1f + stretchFactor * 0.6f
            val currentTransformOrigin = when {
                isStretchingNext -> TransformOrigin(1f, 0.5f) // Anchor right edge, stretch left
                isStretchingPrev -> TransformOrigin(0f, 0.5f) // Anchor left edge, stretch right
                else -> TransformOrigin.Center
            }

            // Previous Song Item Content (entering from left when swiping right)
            if (offset > 0f && (state.hasPrevious || prevItem != null)) {
                MiniPlayerItemContent(
                    title = prevTitle,
                    artist = prevArtist,
                    artworkUri = prevArtworkUri,
                    showRecord = showRecord,
                    rotation = internalRotation,
                    textAlpha = textAlpha,
                    modifier = Modifier.graphicsLayer {
                        translationX = -width + offset
                    }
                )
            }

            // Next Song Item Content (entering from right when swiping left)
            if (offset < 0f && (state.hasNext || nextItem != null)) {
                MiniPlayerItemContent(
                    title = nextTitle,
                    artist = nextArtist,
                    artworkUri = nextArtworkUri,
                    showRecord = showRecord,
                    rotation = internalRotation,
                    textAlpha = textAlpha,
                    modifier = Modifier.graphicsLayer {
                        translationX = width + offset
                    }
                )
            }

            // Current Song Content
            MiniPlayerItemContent(
                title = state.trackTitle,
                artist = state.artist,
                artworkUri = state.artworkUri,
                showRecord = showRecord,
                rotation = internalRotation,
                textAlpha = textAlpha,
                onPositioned = onPositioned,
                modifier = Modifier.graphicsLayer {
                    translationX = offset
                    scaleX = currentScaleX
                    transformOrigin = currentTransformOrigin
                }
            )

            // Bottom Progress Bar (Stationary)
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

@Composable
private fun MiniPlayerItemContent(
    title: String,
    artist: String,
    artworkUri: Uri?,
    showRecord: Boolean,
    rotation: Float,
    textAlpha: Float,
    modifier: Modifier = Modifier,
    onPositioned: ((Offset) -> Unit)? = null
) {
    Row(
        modifier = modifier
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
            val displayArtist = remember(artist, title) {
                collectArtistNames(artist, title).joinArtistsForDisplay()
            }

            Text(
                text = title,
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
                .graphicsLayer { rotationZ = rotation % 360f }
                .clip(CircleShape)
                .then(
                    if (onPositioned != null) {
                        Modifier.onGloballyPositioned { coords ->
                            onPositioned.invoke(coords.positionInRoot())
                        }
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun calculateEffectiveOffset(
    rawOffset: Float,
    width: Float,
    hasPrevious: Boolean,
    hasNext: Boolean
): Float {
    if (width <= 0f) return rawOffset
    return when {
        rawOffset < 0f && !hasNext -> {
            -rubberBandDistance(-rawOffset, width)
        }
        rawOffset > 0f && !hasPrevious -> {
            rubberBandDistance(rawOffset, width)
        }
        else -> rawOffset
    }
}

private fun rubberBandDistance(distance: Float, maxDistance: Float): Float {
    if (distance <= 0f || maxDistance <= 0f) return 0f
    val limit = maxDistance * 0.4f
    return limit * (1f - (1f / (1f + distance * 0.55f / limit)))
}
