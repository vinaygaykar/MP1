package io.github.vinaygaykar.mp1.ui.library

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.components.RollingNumberText
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.itemsIndexed as lazyRowItemsIndexed

/**
 * Unified multi-selection overlay component for songs category.
 * In compact mode (isExpanded = false), renders a pill-shaped floating top/side bar displaying circular artwork avatars and count.
 * In expanded mode (isExpanded = true), expands to fill the screen with a Haze backdrop blur,
 * rendering layout adapted for Portrait, Landscape (2-pane side-by-side), or Compact mode.
 */
@Composable
fun SongsMultiSelectOverlay(
    songs: List<Song>,
    visible: Boolean = songs.isNotEmpty(),
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    bottomPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onPlayNext: (List<Song>) -> Unit = {},
    onAddToPlaylist: (List<Song>) -> Unit = {},
    onAddToQueue: (List<Song>) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isCompact = !isLandscape && configuration.screenHeightDp < 600

    val displaySongs = remember(songs, isLandscape, isExpanded) {
        if (isLandscape && !isExpanded) {
            songs.asReversed().take(2)
        } else {
            songs.asReversed().take(3)
        }
    }

    val transition = updateTransition(targetState = isExpanded, label = "multiSelectExpansion")

    val animationSpec = tween<Dp>(durationMillis = 350, easing = FastOutSlowInEasing)
    val avatarSize by animateDpAsState(
        targetValue = if (isExpanded) 72.dp else if (isCompact) 44.dp else 56.dp,
        animationSpec = animationSpec,
        label = "avatarSize"
    )
    val overlapOffset by animateDpAsState(
        targetValue = if (isExpanded) 40.dp else if (isCompact) 24.dp else 32.dp,
        animationSpec = animationSpec,
        label = "overlapOffset"
    )

    val expansionFraction by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "expansionFraction"
    ) { expanded -> if (expanded) 1f else 0f }

    val cornerPercent by transition.animateInt(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "overlayCornerPercent"
    ) { expanded -> if (expanded) 0 else 50 }

    val borderWidth by transition.animateDp(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "overlayBorderWidth"
    ) { expanded -> if (expanded) 0.dp else 1.dp }

    val borderAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "overlayBorderAlpha"
    ) { expanded -> if (expanded) 0f else 0.35f }

    // Header Content Animation (Slide UP & Fade Away when expanding, Slide DOWN & Fade In when collapsing)
    val headerAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 160, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 200, delayMillis = 120, easing = FastOutSlowInEasing)
            }
        },
        label = "headerAlpha"
    ) { expanded -> if (expanded) 0f else 1f }

    val headerOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "headerOffsetY"
    ) { expanded -> if (expanded) (-40).dp else 0.dp }

    // Expanded View Content Animation (Slide UP & Fade In when expanding, Slide DOWN & Fade Out when collapsing)
    val expandedAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 220, delayMillis = 100, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 150, easing = FastOutSlowInEasing)
            }
        },
        label = "expandedAlpha"
    ) { expanded -> if (expanded) 1f else 0f }

    val expandedOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
        label = "expandedOffsetY"
    ) { expanded -> if (expanded) 0.dp else 40.dp }

    val overlayShape = RoundedCornerShape(cornerPercent)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val coroutineScope = rememberCoroutineScope()
    var titleClearJob by remember { mutableStateOf<Job?>(null) }
    val deselectedSongIds = remember { mutableStateListOf<String>() }
    var highlightedSongTitle by remember { mutableStateOf<String?>(null) }

    val totalAnimatedCount = remember(songs) { songs.size.coerceAtMost(MAX_ANIMATED_ITEMS_COUNT) }
    val revealedIndexLimit by animateIntAsState(
        targetValue = if (isExpanded) totalAnimatedCount else 0,
        animationSpec = tween(
            durationMillis = (totalAnimatedCount * INITIAL_STEP_DELAY_MS.toInt()).coerceIn(
                200,
                1200
            ),
            easing = LinearEasing
        ),
        label = "revealedIndexLimit"
    )

    val showSongTitleWithTimeout: (String) -> Unit = { title ->
        titleClearJob?.cancel()
        highlightedSongTitle = title
        titleClearJob = coroutineScope.launch {
            delay(SONG_TITLE_CLEAR_DELAY_MS)
            highlightedSongTitle = null
        }
    }

    LaunchedEffect(songs, isExpanded) {
        if (!isExpanded) {
            deselectedSongIds.clear()
            highlightedSongTitle = null
            titleClearJob?.cancel()
        }
    }

    val activeSongs = remember(songs, deselectedSongIds.toList()) {
        songs.filter { it.id !in deselectedSongIds }
    }

    val onToggleDeselect: (Song) -> Unit = { song ->
        if (song.id in deselectedSongIds) {
            deselectedSongIds.remove(song.id)
        } else {
            deselectedSongIds.add(song.id)
        }
    }

    var collapsedWidthPx by remember { mutableStateOf(0) }
    var collapsedHeightPx by remember { mutableStateOf(0) }

    val measuredCollapsedWidthDp = if (collapsedWidthPx > 0) {
        with(density) { collapsedWidthPx.toDp() }
    } else {
        if (isLandscape) 72.dp else if (isCompact) 140.dp else 170.dp
    }

    val measuredCollapsedHeightDp = if (collapsedHeightPx > 0) {
        with(density) { collapsedHeightPx.toDp() }
    } else {
        if (isLandscape) 160.dp else if (isCompact) 48.dp else 56.dp
    }

    AnimatedVisibility(
        visible = visible && displaySongs.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = if (!isExpanded && isLandscape) {
            modifier.fillMaxSize()
        } else {
            modifier.fillMaxWidth()
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLandscape) Alignment.TopEnd else Alignment.TopCenter
        ) {
            val screenWidth = this.maxWidth
            val screenHeight = this.maxHeight

            val targetTopPadding = if (isCompact) 8.dp else 12.dp
            val targetSidePadding = if (isCompact) 12.dp else 16.dp

            val animatedTopPadding = lerpDp(targetTopPadding, 0.dp, expansionFraction)
            val animatedSidePadding = lerpDp(targetSidePadding, 0.dp, expansionFraction)

            val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val animatedStatusBarsPadding = lerpDp(statusBarsTop, 0.dp, expansionFraction)

            val containerModifier = if (expansionFraction == 0f) {
                if (isLandscape) {
                    Modifier
                        .statusBarsPadding()
                        .displayCutoutPadding()
                        .padding(top = targetTopPadding, end = targetSidePadding)
                        .wrapContentSize(Alignment.TopEnd)
                } else {
                    Modifier
                        .statusBarsPadding()
                        .padding(top = targetTopPadding, start = targetSidePadding, end = targetSidePadding)
                        .wrapContentSize(Alignment.TopCenter)
                }
            } else {
                val animatedWidth = lerpDp(measuredCollapsedWidthDp, screenWidth, expansionFraction)
                val animatedHeight = lerpDp(measuredCollapsedHeightDp, screenHeight, expansionFraction)
                Modifier
                    .padding(
                        top = animatedStatusBarsPadding + animatedTopPadding,
                        start = if (isLandscape) 0.dp else animatedSidePadding,
                        end = animatedSidePadding
                    )
                    .width(animatedWidth)
                    .height(animatedHeight)
            }

            Box(
                modifier = containerModifier
                    .clip(overlayShape)
                    .border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha),
                        shape = overlayShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isExpanded) {
                                onClick?.invoke()
                            }
                        }
                    )
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = containerColor,
                                    blurRadius = 12.dp,
                                    tint = HazeTint(
                                        containerColor.copy(
                                            alpha = lerpFloat(0.35f, 0.20f, expansionFraction)
                                        )
                                    )
                                )
                            )
                        } else {
                            Modifier.background(
                                containerColor.copy(alpha = lerpFloat(0.85f, 0.95f, expansionFraction)),
                                overlayShape
                            )
                        }
                    )
            ) {
                // Header (Collapsed Preview)
                if (headerAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = headerAlpha
                                translationY = headerOffsetY.toPx()
                            }
                            .onGloballyPositioned { coordinates ->
                                if (!isExpanded && coordinates.size.width > 0 && coordinates.size.height > 0) {
                                    collapsedWidthPx = coordinates.size.width
                                    collapsedHeightPx = coordinates.size.height
                                }
                            }
                    ) {
                        CollapsedMultiSelectHeader(
                            songs = songs,
                            displaySongs = displaySongs,
                            avatarSize = avatarSize,
                            overlapOffset = overlapOffset,
                            isCompact = isCompact,
                            isLandscape = isLandscape
                        )
                    }
                }

                // Expanded View Content
                if (expandedAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = expandedAlpha
                                translationY = expandedOffsetY.toPx()
                            }
                    ) {
                        if (isLandscape) {
                            SongsMultiSelectOverlayLandscape(
                                songs = songs,
                                activeSongs = activeSongs,
                                deselectedSongIds = deselectedSongIds,
                                revealedIndexLimit = revealedIndexLimit,
                                highlightedSongTitle = highlightedSongTitle,
                                bottomPadding = bottomPadding,
                                onToggleDeselect = onToggleDeselect,
                                onShowSongTitle = showSongTitleWithTimeout,
                                onPlayNext = onPlayNext,
                                onAddToPlaylist = onAddToPlaylist,
                                onAddToQueue = onAddToQueue,
                                onDismiss = onDismiss
                            )
                        } else if (isCompact) {
                            SongsMultiSelectOverlayCompact(
                                songs = songs,
                                activeSongs = activeSongs,
                                deselectedSongIds = deselectedSongIds,
                                revealedIndexLimit = revealedIndexLimit,
                                highlightedSongTitle = highlightedSongTitle,
                                bottomPadding = bottomPadding,
                                onToggleDeselect = onToggleDeselect,
                                onShowSongTitle = showSongTitleWithTimeout,
                                onPlayNext = onPlayNext,
                                onAddToPlaylist = onAddToPlaylist,
                                onAddToQueue = onAddToQueue,
                                onDismiss = onDismiss
                            )
                        } else {
                            SongsMultiSelectOverlayPortrait(
                                songs = songs,
                                activeSongs = activeSongs,
                                deselectedSongIds = deselectedSongIds,
                                revealedIndexLimit = revealedIndexLimit,
                                highlightedSongTitle = highlightedSongTitle,
                                bottomPadding = bottomPadding,
                                onToggleDeselect = onToggleDeselect,
                                onShowSongTitle = showSongTitleWithTimeout,
                                onPlayNext = onPlayNext,
                                onAddToPlaylist = onAddToPlaylist,
                                onAddToQueue = onAddToQueue,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    return (start.value + (stop.value - start.value) * fraction.coerceIn(0f, 1f)).dp
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

private const val ITEM_ANIMATION_DURATION_MS = 150
private const val INITIAL_STEP_DELAY_MS = 50L
private const val MAX_ANIMATED_ITEMS_COUNT = 25
private const val SONG_TITLE_CLEAR_DELAY_MS = 3000L

/* -----------------------------------------------------------------------------------------
 * Sub-composables
 * ----------------------------------------------------------------------------------------- */

@Composable
private fun CollapsedMultiSelectHeader(
    songs: List<Song>,
    displaySongs: List<Song>,
    avatarSize: Dp,
    overlapOffset: Dp,
    isCompact: Boolean = false,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val targetAvatarSpan = avatarSize + overlapOffset * (displaySongs.size - 1).coerceAtLeast(0)
    val animatedAvatarSpan by animateDpAsState(
        targetValue = targetAvatarSpan,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "collapsedAvatarSpan"
    )

    if (!isLandscape) {
        // Horizontal Pill Header (Portrait / Compact)
        Box(
            modifier = modifier
                .padding(
                    start = if (isCompact) 14.dp else 18.dp,
                    end = if (isCompact) 18.dp else 22.dp,
                    top = if (isCompact) 8.dp else 12.dp,
                    bottom = if (isCompact) 8.dp else 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(animatedAvatarSpan)
                        .height(avatarSize),
                    contentAlignment = Alignment.CenterStart
                ) {
                    displaySongs.forEachIndexed { index, song ->
                        key(song.id) {
                            val entrySlideAnim = remember { Animatable(-avatarSize.value) }
                            val entryAlphaAnim = remember { Animatable(0f) }

                            LaunchedEffect(song.id) {
                                launch {
                                    entrySlideAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                }
                                launch {
                                    entryAlphaAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                }
                            }

                            val animatedOffset by animateDpAsState(
                                targetValue = overlapOffset * index,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                label = "avatarOffsetX"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = animatedOffset + entrySlideAnim.value.dp)
                                    .graphicsLayer {
                                        alpha = entryAlphaAnim.value
                                    }
                                    .size(avatarSize)
                                    .zIndex((displaySongs.size - index).toFloat())
                                    .border(
                                        width = if (isCompact) 2.dp else 2.5.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.artworkUri != null) {
                                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.artworkUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        error = musicPlaceholder,
                                        placeholder = musicPlaceholder,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(if (isCompact) 11.dp else 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

                RollingNumberText(
                    count = songs.size,
                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        // Vertical Pill Header (Landscape Collapsed - 2 Artworks Preview)
        Box(
            modifier = modifier
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 18.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. Count first
                RollingNumberText(
                    count = songs.size,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Artworks stacked vertically below count (2 preview items in landscape)
                Box(
                    modifier = Modifier
                        .width(avatarSize)
                        .height(animatedAvatarSpan),
                    contentAlignment = Alignment.TopCenter
                ) {
                    displaySongs.forEachIndexed { index, song ->
                        key(song.id) {
                            val entrySlideAnim = remember { Animatable(-avatarSize.value) }
                            val entryAlphaAnim = remember { Animatable(0f) }

                            LaunchedEffect(song.id) {
                                launch {
                                    entrySlideAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                }
                                launch {
                                    entryAlphaAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                }
                            }

                            val animatedOffset by animateDpAsState(
                                targetValue = overlapOffset * index,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                label = "avatarOffsetY"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(y = animatedOffset + entrySlideAnim.value.dp)
                                    .graphicsLayer {
                                        alpha = entryAlphaAnim.value
                                    }
                                    .size(avatarSize)
                                    .zIndex((displaySongs.size - index).toFloat())
                                    .border(
                                        2.5.dp,
                                        MaterialTheme.colorScheme.surface,
                                        CircleShape
                                    )
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.artworkUri != null) {
                                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.artworkUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        error = musicPlaceholder,
                                        placeholder = musicPlaceholder,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedCountHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RollingNumberText(
            count = count,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (count == 1) "song selected" else "songs selected",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SongTitleMarquee(
    highlightedSongTitle: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = highlightedSongTitle ?: "Tap to remove a song, hold to view",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (highlightedSongTitle != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.7f
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
private fun ActionButtonsGroup(
    activeSongs: List<Song>,
    bottomPadding: Dp,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeSongs.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action 1: Play next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onPlayNext(activeSongs)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Play next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action 2: Add to Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onAddToQueue(activeSongs)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add to queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action 3: Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onAddToPlaylist(activeSongs)
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Single grid item representing a selected song artwork avatar in the expanded grid.
 * Animates in as index < revealedIndexLimit.
 * Slides vertically (offsetY) for Portrait/Compact, or horizontally (offsetX) for Landscape orientation.
 * Shows disabled effect when deselected.
 * Short click toggles deselection; long press displays song title for 3s.
 */
@Composable
private fun StaggeredGridArtworkAvatar(
    song: Song,
    index: Int,
    isRevealed: Boolean,
    isDeselected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    animateHorizontal: Boolean = false
) {
    val isBeyondThreshold = index >= MAX_ANIMATED_ITEMS_COUNT
    val shouldShow = isRevealed || isBeyondThreshold

    val animationSpec = if (isBeyondThreshold) snap<Float>() else tween<Float>(
        durationMillis = ITEM_ANIMATION_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    val animationDpSpec = if (isBeyondThreshold) snap<Dp>() else tween<Dp>(
        durationMillis = ITEM_ANIMATION_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    val alpha by animateFloatAsState(
        targetValue = if (!shouldShow) 0f else if (isDeselected) 0.35f else 1f,
        animationSpec = animationSpec,
        label = "avatarAlpha"
    )
    val offsetVal by animateDpAsState(
        targetValue = if (shouldShow) 0.dp else 24.dp,
        animationSpec = animationDpSpec,
        label = "avatarOffset"
    )

    val offsetX = if (animateHorizontal) offsetVal else 0.dp
    val offsetY = if (animateHorizontal) 0.dp else offsetVal

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                this.alpha = alpha
            }
            .size(54.dp)
            .aspectRatio(1f)
            .border(
                width = 2.5.dp,
                color = if (isDeselected) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (song.artworkUri != null) {
            val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                error = musicPlaceholder,
                placeholder = musicPlaceholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }

        if (isDeselected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
            )
        }
    }
}

/* -----------------------------------------------------------------------------------------
 * Mode-Specific Views
 * ----------------------------------------------------------------------------------------- */

@Composable
private fun SongsMultiSelectOverlayPortrait(
    songs: List<Song>,
    activeSongs: List<Song>,
    deselectedSongIds: List<String>,
    revealedIndexLimit: Int,
    highlightedSongTitle: String?,
    bottomPadding: Dp,
    onToggleDeselect: (Song) -> Unit,
    onShowSongTitle: (String) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val actualRows = songs.size.coerceAtMost(5)
    val gridHeight = (54.dp * actualRows) + (10.dp * (actualRows - 1).coerceAtLeast(0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Artwork Header Group + Count Text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SelectedCountHeader(count = activeSongs.size)

                Spacer(modifier = Modifier.height(36.dp))

                if (songs.size <= 5) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lazyRowItemsIndexed(
                            items = songs,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isDeselected = song.id in deselectedSongIds
                            StaggeredGridArtworkAvatar(
                                song = song,
                                index = index,
                                isRevealed = index < revealedIndexLimit,
                                isDeselected = isDeselected,
                                animateHorizontal = false,
                                onClick = { onToggleDeselect(song) },
                                onLongClick = { onShowSongTitle(song.title) }
                            )
                        }
                    }
                } else {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = songs,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isDeselected = song.id in deselectedSongIds
                            StaggeredGridArtworkAvatar(
                                song = song,
                                index = index,
                                isRevealed = index < revealedIndexLimit,
                                isDeselected = isDeselected,
                                animateHorizontal = false,
                                onClick = { onToggleDeselect(song) },
                                onLongClick = { onShowSongTitle(song.title) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SongTitleMarquee(highlightedSongTitle = highlightedSongTitle)

        Spacer(modifier = Modifier.height(24.dp))

        ActionButtonsGroup(
            activeSongs = activeSongs,
            bottomPadding = bottomPadding,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SongsMultiSelectOverlayCompact(
    songs: List<Song>,
    activeSongs: List<Song>,
    deselectedSongIds: List<String>,
    revealedIndexLimit: Int,
    highlightedSongTitle: String?,
    bottomPadding: Dp,
    onToggleDeselect: (Song) -> Unit,
    onShowSongTitle: (String) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val actualRows = songs.size.coerceAtMost(3)
    val gridHeight = (54.dp * actualRows) + (10.dp * (actualRows - 1).coerceAtLeast(0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SelectedCountHeader(count = activeSongs.size)

                Spacer(modifier = Modifier.height(20.dp))

                if (songs.size <= 3) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lazyRowItemsIndexed(
                            items = songs,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isDeselected = song.id in deselectedSongIds
                            StaggeredGridArtworkAvatar(
                                song = song,
                                index = index,
                                isRevealed = index < revealedIndexLimit,
                                isDeselected = isDeselected,
                                animateHorizontal = false,
                                onClick = { onToggleDeselect(song) },
                                onLongClick = { onShowSongTitle(song.title) }
                            )
                        }
                    }
                } else {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = songs,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isDeselected = song.id in deselectedSongIds
                            StaggeredGridArtworkAvatar(
                                song = song,
                                index = index,
                                isRevealed = index < revealedIndexLimit,
                                isDeselected = isDeselected,
                                animateHorizontal = false,
                                onClick = { onToggleDeselect(song) },
                                onLongClick = { onShowSongTitle(song.title) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SongTitleMarquee(highlightedSongTitle = highlightedSongTitle)

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtonsGroup(
            activeSongs = activeSongs,
            bottomPadding = bottomPadding,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SongsMultiSelectOverlayLandscape(
    songs: List<Song>,
    activeSongs: List<Song>,
    deselectedSongIds: List<String>,
    revealedIndexLimit: Int,
    highlightedSongTitle: String?,
    bottomPadding: Dp,
    onToggleDeselect: (Song) -> Unit,
    onShowSongTitle: (String) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rightPaneScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding()
    ) {
        // Left Pane: Column with Static Song Title at top, Grid below
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Song Title Header (moved down from top screen edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SongTitleMarquee(highlightedSongTitle = highlightedSongTitle)
            }

            // 2. Grid starting AFTER song title text
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = bottomPadding + 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = songs,
                    key = { index, song -> "${song.id}_$index" }
                ) { index, song ->
                    val isDeselected = song.id in deselectedSongIds
                    StaggeredGridArtworkAvatar(
                        song = song,
                        index = index,
                        isRevealed = index < revealedIndexLimit,
                        isDeselected = isDeselected,
                        animateHorizontal = true,
                        onClick = { onToggleDeselect(song) },
                        onLongClick = { onShowSongTitle(song.title) }
                    )
                }
            }
        }

        // Right Pane: Selected Count + Action Buttons
        // Vertically centered when content fits, aligned to top when content overflows
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val paneMinHeight = this.maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rightPaneScrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = paneMinHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SelectedCountHeader(count = activeSongs.size)

                        Spacer(modifier = Modifier.height(24.dp))

                        ActionButtonsGroup(
                            activeSongs = activeSongs,
                            bottomPadding = bottomPadding,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = onAddToPlaylist,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

/* -----------------------------------------------------------------------------------------
 * Previews for SongsMultiSelectOverlay
 * ----------------------------------------------------------------------------------------- */

private val sample32PreviewSongs = (1..32).map { index ->
    Song(
        id = "$index",
        uri = android.net.Uri.EMPTY,
        title = "Lorem Ipsum $index",
        artist = "Artist $index",
        album = "Album $index",
        durationMs = 200000L + index * 1000L,
        trackNumber = index,
        discNumber = 1,
        year = 2024,
        artworkUri = null,
        fileName = "lorem_ipsum_$index.mp3",
        lastModified = 0L
    )
}

@Preview(
    name = "Portrait Light",
    group = "SongsMultiSelectOverlay - Collapsed",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "SongsMultiSelectOverlay - Collapsed",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "SongsMultiSelectOverlay - Collapsed",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "SongsMultiSelectOverlay - Collapsed",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Compact / Split Light",
    group = "SongsMultiSelectOverlay - Collapsed",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongsMultiSelectOverlayCollapsedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongsMultiSelectOverlay(
                songs = sample32PreviewSongs.take(3),
                visible = true,
                isExpanded = false
            )
        }
    }
}

@Preview(
    name = "Portrait Light",
    group = "SongsMultiSelectOverlay - Expanded 32 Songs",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "SongsMultiSelectOverlay - Expanded 32 Songs",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "SongsMultiSelectOverlay - Expanded 32 Songs",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "SongsMultiSelectOverlay - Expanded 32 Songs",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Compact / Split Light",
    group = "SongsMultiSelectOverlay - Expanded 32 Songs",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongsMultiSelectOverlayExpandedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongsMultiSelectOverlay(
                songs = sample32PreviewSongs,
                visible = true,
                isExpanded = true
            )
        }
    }
}

@Preview(
    name = "Portrait Light",
    group = "SongsMultiSelectOverlay - Expanded 5 Songs",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "SongsMultiSelectOverlay - Expanded 5 Songs",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SongsMultiSelectOverlayExpanded5SongsPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongsMultiSelectOverlay(
                songs = sample32PreviewSongs.take(5),
                visible = true,
                isExpanded = true
            )
        }
    }
}
