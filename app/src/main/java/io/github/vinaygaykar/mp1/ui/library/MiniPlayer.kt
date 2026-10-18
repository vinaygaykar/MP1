package io.github.vinaygaykar.mp1.ui.library

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
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

    val hasPrevPage = remember(state.queue.size, state.currentIndex, state.hasPrevious, state.repeatMode) {
        if (state.queue.size <= 1) false
        else if (state.currentIndex > 0) true
        else if (state.currentIndex == 0) state.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ALL
        else state.hasPrevious
    }

    val hasNextPage = remember(state.queue.size, state.currentIndex, state.hasNext, state.repeatMode) {
        if (state.queue.size <= 1) false
        else if (state.currentIndex in 0 until state.queue.size - 1) true
        else if (state.currentIndex == state.queue.size - 1) state.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ALL
        else state.hasNext
    }

    val prevItem = remember(state.queue, state.currentIndex, hasPrevPage) {
        if (!hasPrevPage || state.queue.size <= 1) null
        else {
            val prevIndex = if (state.currentIndex > 0) {
                state.currentIndex - 1
            } else {
                state.queue.size - 1
            }
            state.queue.getOrNull(prevIndex)
        }
    }

    val nextItem = remember(state.queue, state.currentIndex, hasNextPage) {
        if (!hasNextPage || state.queue.size <= 1) null
        else {
            val nextIndex = if ((state.currentIndex in state.queue.indices) && (state.currentIndex + 1 < state.queue.size)) {
                state.currentIndex + 1
            } else {
                0
            }
            state.queue.getOrNull(nextIndex)
        }
    }

    val nextTitle = remember(nextItem) { nextItem?.mediaMetadata?.title?.toString() ?: "" }
    val nextArtist = remember(nextItem) { nextItem?.mediaMetadata?.artist?.toString() ?: "" }
    val nextArtworkUri = remember(nextItem) { nextItem?.mediaMetadata?.artworkUri }

    val prevTitle = remember(prevItem) { prevItem?.mediaMetadata?.title?.toString() ?: "" }
    val prevArtist = remember(prevItem) { prevItem?.mediaMetadata?.artist?.toString() ?: "" }
    val prevArtworkUri = remember(prevItem) { prevItem?.mediaMetadata?.artworkUri }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val currentTrackPageIndex = if (hasPrevPage) 1 else 0
    val totalPageCount = 1 + (if (hasPrevPage) 1 else 0) + (if (hasNextPage) 1 else 0)

    val pagerState = rememberPagerState(
        initialPage = currentTrackPageIndex,
        pageCount = { totalPageCount }
    )

    val boundaryOffsetAnimatable = remember { Animatable(0f) }

    LaunchedEffect(state.currentIndex, state.trackTitle, hasPrevPage, hasNextPage) {
        if (pagerState.currentPage != currentTrackPageIndex) {
            pagerState.scrollToPage(currentTrackPageIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage, currentTrackPageIndex) {
        if (pagerState.settledPage < currentTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            actions.onPrevious()
        } else if (pagerState.settledPage > currentTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            actions.onNext()
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && boundaryOffsetAnimatable.value != 0f) {
            boundaryOffsetAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            )
        }
    }

    val boundaryNestedScrollConnection = remember(hasPrevPage, hasNextPage, totalPageCount, currentTrackPageIndex) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = boundaryOffsetAnimatable.value
                if (currentOffset == 0f) return Offset.Zero

                val deltaX = available.x
                if ((currentOffset > 0f && deltaX < 0f) || (currentOffset < 0f && deltaX > 0f)) {
                    val newOffset = if (currentOffset > 0f) {
                        (currentOffset + deltaX).coerceAtLeast(0f)
                    } else {
                        (currentOffset + deltaX).coerceAtMost(0f)
                    }
                    val consumedX = newOffset - currentOffset
                    coroutineScope.launch {
                        boundaryOffsetAnimatable.snapTo(newOffset)
                    }
                    return Offset(consumedX, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaX = available.x
                if (deltaX == 0f) return Offset.Zero

                val isAtPrevBoundary = !hasPrevPage && deltaX > 0f && pagerState.currentPage == 0
                val isAtNextBoundary = !hasNextPage && deltaX < 0f && pagerState.currentPage == totalPageCount - 1

                if (isAtPrevBoundary || isAtNextBoundary) {
                    val currentOffset = boundaryOffsetAnimatable.value
                    val newOffset = currentOffset + deltaX
                    coroutineScope.launch {
                        boundaryOffsetAnimatable.snapTo(newOffset)
                    }
                    if (abs(currentOffset) < 15f && abs(newOffset) >= 15f) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    return Offset(deltaX, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (boundaryOffsetAnimatable.value != 0f) {
                    boundaryOffsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (boundaryOffsetAnimatable.value != 0f) {
                    boundaryOffsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    val rawBoundaryOffset = boundaryOffsetAnimatable.value
    val width = if (containerWidthPx > 0f) containerWidthPx else 400f
    val effectiveBoundaryOffset = calculateEffectiveOffset(
        rawOffset = rawBoundaryOffset,
        width = width,
        hasPrevious = state.hasPrevious,
        hasNext = state.hasNext
    )

    val isStretchingNext = effectiveBoundaryOffset < 0f && !state.hasNext
    val isStretchingPrev = effectiveBoundaryOffset > 0f && !state.hasPrevious

    val stretchFactor = if (isStretchingNext || isStretchingPrev) {
        (abs(effectiveBoundaryOffset) / width).coerceIn(0f, 0.4f)
    } else 0f

    val currentScaleX = 1f + stretchFactor * 0.6f
    val currentTransformOrigin = when {
        isStretchingNext -> TransformOrigin(1f, 0.5f)
        isStretchingPrev -> TransformOrigin(0f, 0.5f)
        else -> TransformOrigin.Center
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
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
            @Suppress("DEPRECATION")
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(boundaryNestedScrollConnection)
                        .graphicsLayer {
                            translationX = effectiveBoundaryOffset
                            scaleX = currentScaleX
                            transformOrigin = currentTransformOrigin
                        }
                ) { page ->
                    val (title, artist, artworkUri) = when {
                        page == currentTrackPageIndex -> Triple(state.trackTitle, state.artist, state.artworkUri)
                        page < currentTrackPageIndex -> Triple(prevTitle, prevArtist, prevArtworkUri)
                        else -> Triple(nextTitle, nextArtist, nextArtworkUri)
                    }

                    MiniPlayerItemContent(
                        title = title,
                        artist = artist,
                        artworkUri = artworkUri,
                        showRecord = showRecord,
                        rotation = internalRotation,
                        textAlpha = textAlpha,
                        onPositioned = if (page == currentTrackPageIndex) onPositioned else null
                    )
                }
            }

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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 28.dp, end = 72.dp)
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

        // Vinyl Record on the right side (constant 56.dp x 56.dp size)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
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
