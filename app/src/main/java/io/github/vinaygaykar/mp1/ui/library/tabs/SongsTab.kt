package io.github.vinaygaykar.mp1.ui.library.tabs

import android.content.res.Configuration
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

private class Ref<T>(var value: T)

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSwipeRight: (Song, Offset, IntSize) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    selectedSongIds: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onSongLongClick: (Song) -> Unit = {},
    headerHeight: Dp = 104.dp
) {
    if (songs.isEmpty()) {
        val topPadding = contentPadding.calculateTopPadding()
        val bottomPadding = contentPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topPadding,
                    bottom = bottomPadding + headerHeight + topPadding,
                    start = 32.dp,
                    end = 32.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No songs found. Try scanning a folder in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                items(songs, key = { it.id }) { song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    val onClickLambda = remember(song.id, isSelectionMode) {
                        { onSongClick(song) }
                    }
                    val onSwipeRightLambda = remember(song.id) {
                        { offset: Offset, size: IntSize -> onSwipeRight(song, offset, size) }
                    }
                    val onLongClickLambda = remember(song.id) {
                        { onSongLongClick(song) }
                    }
                    SongListItem(
                        song = song,
                        onClick = onClickLambda,
                        onSwipeRight = onSwipeRightLambda,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = onLongClickLambda
                    )
                }
            }
            SimpleScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                listState = listState
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    isFastScrolling: Boolean = false,
    onSwipeRight: (Offset, IntSize) -> Unit = { _, _ -> },
    onSwipeRightNoArgs: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    val displayArtist = remember(song.artistsDisplayString, song.artist) {
        song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
    }
    val artworkCoordinatesRef = remember { Ref<LayoutCoordinates?>(null) }

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
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isFastScrolling || imageRequest == null) {
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
                            .clip(CircleShape)
                            .onGloballyPositioned { coords ->
                                artworkCoordinatesRef.value = coords
                            },
                        contentScale = ContentScale.Crop
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                IconButton(
                    onClick = {
                        val coords = artworkCoordinatesRef.value
                        val offset = coords?.positionInRoot() ?: Offset.Zero
                        val size = coords?.size ?: IntSize(100, 100)
                        onSwipeRight(offset, size)
                        onSwipeRightNoArgs?.invoke()
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
            )
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

private val sampleSongs = listOf(
    Song(
        id = "1",
        uri = Uri.EMPTY,
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationMs = 243000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2011,
        artworkUri = null,
        fileName = "midnight_city.mp3",
        lastModified = 0L,
        artistsDisplayString = "M83"
    ),
    Song(
        id = "2",
        uri = Uri.EMPTY,
        title = "Starboy",
        artist = "The Weeknd",
        album = "Starboy",
        durationMs = 230000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2016,
        artworkUri = null,
        fileName = "starboy.mp3",
        lastModified = 0L,
        artistsDisplayString = "The Weeknd, Daft Punk"
    ),
    Song(
        id = "3",
        uri = Uri.EMPTY,
        title = "Get Lucky",
        artist = "Daft Punk",
        album = "Random Access Memories",
        durationMs = 248000L,
        trackNumber = 3,
        discNumber = 1,
        year = 2013,
        artworkUri = null,
        fileName = "get_lucky.mp3",
        lastModified = 0L,
        artistsDisplayString = "Daft Punk ft. Pharrell Williams"
    )
)

@Preview(
    name = "Portrait Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SongsTabPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongList(
                songs = sampleSongs,
                onSongClick = {},
                onSwipeRight = { _, _, _ -> }
            )
        }
    }
}

