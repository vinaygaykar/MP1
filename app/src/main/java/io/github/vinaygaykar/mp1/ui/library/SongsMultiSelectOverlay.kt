package io.github.vinaygaykar.mp1.ui.library

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed as lazyRowItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay

/**
 * Unified multi-selection overlay component for songs category.
 * In compact mode (isExpanded = false), renders an edge-to-edge top bar displaying circular artwork avatars and count.
 * In expanded mode (isExpanded = true), expands to fill the screen with a Haze backdrop blur,
 * where the artwork avatars and count occupy 75% of screen height, and the remaining space displays
 * action buttons ("Play all", "Add to queue", "Playlist").
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
    val displaySongs = remember(songs) { songs.asReversed().take(3) }
    val animationSpec = tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
    val avatarSize by animateDpAsState(
        targetValue = if (isExpanded) 72.dp else 56.dp,
        animationSpec = animationSpec,
        label = "avatarSize"
    )
    val overlapOffset by animateDpAsState(
        targetValue = if (isExpanded) 40.dp else 32.dp,
        animationSpec = animationSpec,
        label = "overlapOffset"
    )
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()
    var titleClearJob by remember { mutableStateOf<Job?>(null) }
    val deselectedSongIds = remember { mutableStateListOf<String>() }
    val animatedSongIds = remember { mutableStateListOf<String>() }
    var highlightedSongTitle by remember { mutableStateOf<String?>(null) }

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
            animatedSongIds.clear()
            titleClearJob?.cancel()
        }
    }

    val activeSongs = remember(songs, deselectedSongIds.toList()) {
        songs.filter { it.id !in deselectedSongIds }
    }

    AnimatedVisibility(
        visible = visible && displaySongs.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isExpanded) Modifier.fillMaxSize() else Modifier)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = FastOutSlowInEasing
                    )
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
                                tint = HazeTint(containerColor.copy(alpha = 0.20f))
                            )
                        )
                    } else {
                        Modifier.background(containerColor.copy(alpha = 0.20f))
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isExpanded) Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState) else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artwork Header Group + Count Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = if (isExpanded) 0.dp else 16.dp, vertical = if (isExpanded) 24.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isExpanded) {
                        // Collapsed Mode: Compact 3-overlapping artwork avatars + rolling count number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(
                                        avatarSize + overlapOffset * (displaySongs.size - 1).coerceAtLeast(
                                            0
                                        )
                                    )
                                    .height(avatarSize),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                displaySongs.forEachIndexed { index, song ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = overlapOffset * index)
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
                                            val musicPlaceholder =
                                                rememberVectorPainter(Icons.Default.MusicNote)
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

                            Spacer(modifier = Modifier.width(16.dp))

                            RollingNumberText(
                                count = songs.size,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Expanded Mode: 2-Row Header Layout
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Row 1: Selected song count + "songs selected" label
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RollingNumberText(
                                    count = activeSongs.size,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (activeSongs.size == 1) "song selected" else "songs selected",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            // Row 2: Single horizontal row for <= 5 songs, or 5-row LazyHorizontalGrid for > 5 songs
                            if (songs.size <= 5) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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
                                            isExpanded = isExpanded,
                                            isDeselected = isDeselected,
                                            hasAlreadyAnimated = song.id in animatedSongIds,
                                            onAnimated = { animatedSongIds.add(song.id) },
                                            onClick = {
                                                if (isDeselected) {
                                                    deselectedSongIds.remove(song.id)
                                                } else {
                                                    deselectedSongIds.add(song.id)
                                                }
                                            },
                                            onLongClick = {
                                                showSongTitleWithTimeout(song.title)
                                            }
                                        )
                                    }
                                }
                            } else {
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(5),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(310.dp),
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
                                            isExpanded = isExpanded,
                                            isDeselected = isDeselected,
                                            hasAlreadyAnimated = song.id in animatedSongIds,
                                            onAnimated = { animatedSongIds.add(song.id) },
                                            onClick = {
                                                if (isDeselected) {
                                                    deselectedSongIds.remove(song.id)
                                                } else {
                                                    deselectedSongIds.add(song.id)
                                                }
                                            },
                                            onLongClick = {
                                                showSongTitleWithTimeout(song.title)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons list + Dynamic Song Title Marquee Text when expanded
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Song Title / Empty State Marquee Text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = highlightedSongTitle ?: "Tap to remove a song, hold to view",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (highlightedSongTitle != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = bottomPadding + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Action 1: Play next
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPlayNext(activeSongs)
                                    onDismiss()
                                },
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
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onAddToQueue(activeSongs)
                                    onDismiss()
                                },
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
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onAddToPlaylist(activeSongs)
                                },
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
        }
    }
}

private const val STAGGERED_ANIMATION_DURATION_MS = 60
private const val MAX_ANIMATED_ITEMS_COUNT = 25
private const val SONG_TITLE_CLEAR_DELAY_MS = 3000L

/**
 * Single grid item representing a selected song artwork avatar in the expanded grid.
 * Animates in sequentially with a constant delay step, sliding up from below.
 * Shows disabled effect when deselected.
 * Short click toggles deselection; long press displays song title for 3s.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StaggeredGridArtworkAvatar(
    song: Song,
    index: Int,
    isExpanded: Boolean,
    isDeselected: Boolean,
    hasAlreadyAnimated: Boolean,
    onAnimated: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBeyondThreshold = index >= MAX_ANIMATED_ITEMS_COUNT || hasAlreadyAnimated
    val delayMs = if (isBeyondThreshold) 0L else index * STAGGERED_ANIMATION_DURATION_MS.toLong()

    var visible by remember(hasAlreadyAnimated) { mutableStateOf(hasAlreadyAnimated) }

    LaunchedEffect(isExpanded, hasAlreadyAnimated) {
        if (isExpanded) {
            if (!hasAlreadyAnimated && delayMs > 0L) {
                delay(delayMs)
            }
            visible = true
            onAnimated()
        } else {
            visible = false
        }
    }

    val animationSpec = if (isBeyondThreshold) snap<Float>() else tween<Float>(durationMillis = STAGGERED_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    val animationDpSpec = if (isBeyondThreshold) snap<Dp>() else tween<Dp>(durationMillis = STAGGERED_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)

    val alpha by animateFloatAsState(
        targetValue = if (!visible) 0f else if (isDeselected) 0.35f else 1f,
        animationSpec = animationSpec,
        label = "avatarAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = animationDpSpec,
        label = "avatarOffsetY"
    )

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .graphicsLayer {
                this.alpha = alpha
            }
            .size(54.dp)
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
 * Previews for SongsMultiSelectOverlay
 * ----------------------------------------------------------------------------------------- */

private val samplePreviewSongs = listOf(
    Song(
        id = "1",
        uri = android.net.Uri.EMPTY,
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationMs = 200000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2020,
        artworkUri = null,
        fileName = "blinding_lights.mp3",
        lastModified = 0L
    ),
    Song(
        id = "2",
        uri = android.net.Uri.EMPTY,
        title = "Starboy",
        artist = "The Weeknd",
        album = "Starboy",
        durationMs = 210000L,
        trackNumber = 2,
        discNumber = 1,
        year = 2016,
        artworkUri = null,
        fileName = "starboy.mp3",
        lastModified = 0L
    ),
    Song(
        id = "3",
        uri = android.net.Uri.EMPTY,
        title = "Save Your Tears",
        artist = "The Weeknd",
        album = "After Hours",
        durationMs = 215000L,
        trackNumber = 3,
        discNumber = 1,
        year = 2020,
        artworkUri = null,
        fileName = "save_your_tears.mp3",
        lastModified = 0L
    )
)

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
                songs = samplePreviewSongs,
                visible = true,
                isExpanded = false
            )
        }
    }
}

@Preview(
    name = "Portrait Light",
    group = "SongsMultiSelectOverlay - Expanded",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "SongsMultiSelectOverlay - Expanded",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "SongsMultiSelectOverlay - Expanded",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "SongsMultiSelectOverlay - Expanded",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Compact / Split Light",
    group = "SongsMultiSelectOverlay - Expanded",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongsMultiSelectOverlayExpandedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongsMultiSelectOverlay(
                songs = samplePreviewSongs,
                visible = true,
                isExpanded = true
            )
        }
    }
}
