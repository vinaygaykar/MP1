package io.github.vinaygaykar.mp1.ui.library

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onPlayAll: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onAddToQueue: () -> Unit = {}
) {
    val displaySongs = remember(songs) { songs.asReversed().take(3) }
    val avatarSize by animateDpAsState(
        targetValue = if (isExpanded) 72.dp else 56.dp,
        label = "avatarSize"
    )
    val overlapOffset by animateDpAsState(
        targetValue = if (isExpanded) 40.dp else 32.dp,
        label = "overlapOffset"
    )
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val expandedHeaderHeight = screenHeight * 0.75f

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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isExpanded) {
                            onDismiss()
                        } else {
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
                    .then(if (isExpanded) Modifier.fillMaxSize().verticalScroll(scrollState) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artwork Header Group + Count Text (75% screen height in expanded mode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isExpanded) Modifier.height(expandedHeaderHeight) else Modifier)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = if (isExpanded) 24.dp else 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(avatarSize + overlapOffset * (displaySongs.size - 1).coerceAtLeast(0))
                                .height(avatarSize),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            displaySongs.forEachIndexed { index, song ->
                                Box(
                                    modifier = Modifier
                                        .offset(x = overlapOffset * index)
                                        .size(avatarSize)
                                        .zIndex((displaySongs.size - index).toFloat())
                                        .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
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
                                            modifier = Modifier.size(if (isExpanded) 36.dp else 28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(if (isExpanded) 20.dp else 16.dp))

                        RollingNumberText(
                            count = songs.size,
                            style = if (isExpanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Action Buttons list when expanded
                if (isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = bottomPadding + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Action 1: Play all
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPlayAll()
                                    onDismiss()
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Play all",
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
                                    onAddToQueue()
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
                                    onAddToPlaylist()
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

/* -----------------------------------------------------------------------------------------
 * Previews for SongsMultiSelectOverlay
 * ----------------------------------------------------------------------------------------- */

private val samplePreviewSongs = listOf(
    Song(id = "1", uri = android.net.Uri.EMPTY, title = "Blinding Lights", artist = "The Weeknd", album = "After Hours", durationMs = 200000L, trackNumber = 1, discNumber = 1, year = 2020, artworkUri = null, fileName = "blinding_lights.mp3", lastModified = 0L),
    Song(id = "2", uri = android.net.Uri.EMPTY, title = "Starboy", artist = "The Weeknd", album = "Starboy", durationMs = 210000L, trackNumber = 2, discNumber = 1, year = 2016, artworkUri = null, fileName = "starboy.mp3", lastModified = 0L),
    Song(id = "3", uri = android.net.Uri.EMPTY, title = "Save Your Tears", artist = "The Weeknd", album = "After Hours", durationMs = 215000L, trackNumber = 3, discNumber = 1, year = 2020, artworkUri = null, fileName = "save_your_tears.mp3", lastModified = 0L)
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
