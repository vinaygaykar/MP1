package io.github.vinaygaykar.mp1.ui.components

import android.content.res.Configuration
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

enum class SongListItemTrailingContent {
    KEBAB_MENU,
    DRAG_HANDLE
}

typealias TrailingContent = SongListItemTrailingContent

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFastScrolling: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showTrackNumber: Boolean = false,
    containerColor: Color = Color.Transparent,
    trailingContent: SongListItemTrailingContent = SongListItemTrailingContent.KEBAB_MENU,
    dragModifier: Modifier = Modifier,
    onDragStart: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onLongClick: () -> Unit = {},
    onMoreClick: (Offset, IntSize) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val view = LocalView.current

    var artworkOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var artworkSizePx by remember { mutableStateOf(IntSize.Zero) }

    val displayArtist = remember(song.artistsDisplayString, song.artist) {
        song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
    }

    val imageRequest = remember(song.artworkUri) {
        if (song.artworkUri == null) null
        else {
            ImageRequest.Builder(context)
                .data(song.artworkUri)
                .size(144, 144)
                .precision(Precision.INEXACT)
                .crossfade(false)
                .build()
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = { Text(displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onGloballyPositioned { coordinates ->
                        artworkOffsetInRoot = coordinates.positionInRoot()
                        artworkSizePx = coordinates.size
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showTrackNumber) {
                    val trackNumberText = remember(song.trackNumber) {
                        song.trackNumber?.toString() ?: "-"
                    }
                    Text(
                        text = trackNumberText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                } else if (isFastScrolling || imageRequest == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        error = musicPlaceholder,
                        placeholder = musicPlaceholder,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        trailingContent = {
            if (isSelectionMode) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                when (trailingContent) {
                    SongListItemTrailingContent.KEBAB_MENU -> {
                        IconButton(
                            onClick = {
                                onMoreClick(artworkOffsetInRoot, artworkSizePx)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SongListItemTrailingContent.DRAG_HANDLE -> {
                        val currentOnDragStart by rememberUpdatedState(onDragStart)
                        val currentOnDrag by rememberUpdatedState(onDrag)
                        val currentOnDragEnd by rememberUpdatedState(onDragEnd)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .then(dragModifier)
                                .then(
                                    if (onDrag != null) {
                                        Modifier.pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                onDragStart = { currentOnDragStart?.invoke() },
                                                onDragEnd = { currentOnDragEnd?.invoke() },
                                                onDragCancel = { currentOnDragEnd?.invoke() },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    currentOnDrag?.invoke(dragAmount)
                                                }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else containerColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
    )
}

private val samplePreviewSong = Song(
    id = "1",
    uri = Uri.EMPTY,
    title = "Lorem ipsum dolor sit amet",
    artist = "Consectetur adipiscing elit",
    album = "Sed do eiusmod tempor",
    durationMs = 230000L,
    trackNumber = 1,
    discNumber = 1,
    year = 2024,
    artworkUri = null,
    fileName = "lorem_ipsum.mp3",
    lastModified = 0L,
    artistsDisplayString = "Lorem Ipsum, Dolor Sit Amet"
)

@Preview(
    name = "Normal - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Normal - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SongListItemNormalPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface {
            SongListItem(
                song = samplePreviewSong,
                onClick = {},
                isSelected = false,
                isSelectionMode = false
            )
        }
    }
}

@Preview(
    name = "Selected - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Selected - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SongListItemSelectedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface {
            SongListItem(
                song = samplePreviewSong,
                onClick = {},
                isSelected = true,
                isSelectionMode = true
            )
        }
    }
}

@Preview(
    name = "Track Number 3 Digits - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongListItemTrackNumberPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface {
            SongListItem(
                song = samplePreviewSong.copy(trackNumber = 103),
                onClick = {},
                showTrackNumber = true
            )
        }
    }
}

@Preview(
    name = "Track Number 4 Digits - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongListItemTrackNumberLongPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface {
            SongListItem(
                song = samplePreviewSong.copy(trackNumber = 1234),
                onClick = {},
                showTrackNumber = true
            )
        }
    }
}

@Preview(
    name = "Drag Handle - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SongListItemDragHandlePreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface {
            SongListItem(
                song = samplePreviewSong,
                onClick = {},
                trailingContent = SongListItemTrailingContent.DRAG_HANDLE
            )
        }
    }
}

