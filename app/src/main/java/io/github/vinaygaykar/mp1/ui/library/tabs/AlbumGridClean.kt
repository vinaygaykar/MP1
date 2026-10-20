package io.github.vinaygaykar.mp1.ui.library.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.components.ExpressiveScrollBar
import io.github.vinaygaykar.mp1.ui.components.extractFastScrollGlyph

/**
 * AlbumGridClean renders a 2-column grid layout of square album art with sharp corners.
 */
@Composable
fun AlbumGridClean(
    albums: List<Album>,
    onAlbumClick: (Album, Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier,
    selectedAlbumId: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (albums.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No albums found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val lazyGridState = rememberLazyGridState()
    var isScrollbarInteracting by remember { mutableStateOf(value = false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = lazyGridState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 28.dp,
                top = 16.dp + contentPadding.calculateTopPadding(),
                bottom = 16.dp + contentPadding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(albums, key = { _, album -> album.id }) { _, album ->
                AlbumCleanCard(
                    album = album,
                    onClick = { offset, size -> onAlbumClick(album, offset, size) },
                    isSelected = album.id == selectedAlbumId,
                )
            }
        }

        // Fast Scrollbar overlay
        ExpressiveScrollBar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(100f)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            gridState = lazyGridState,
            onInteractingChanged = { interacting ->
                isScrollbarInteracting = interacting
            },
            dragLabelProvider = { index ->
                if (index in albums.indices) {
                    extractFastScrollGlyph(albums[index].name)
                } else null
            },
        )
    }
}

/**
 * Square album art item with sharp corners (no text, no card container).
 */
@Composable
fun AlbumCleanCard(
    album: Album,
    onClick: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val context = LocalContext.current
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backgroundColor = if (isSelected) {
        Color(0xFFFFDE38) // Accent yellow
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(backgroundColor)
            .onGloballyPositioned { coords ->
                itemCoordinates = coords
            }
            .clickable {
                val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                val size = itemCoordinates?.size ?: IntSize.Zero
                onClick(offset, size)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (album.artworkUri != null) {
            val request = remember(album.artworkUri) {
                ImageRequest.Builder(context)
                    .data(album.artworkUri)
                    .size(300, 300)
                    .crossfade(true)
                    .build()
            }
            val albumPlaceholder = rememberVectorPainter(Icons.Default.Album)
            AsyncImage(
                model = request,
                contentDescription = album.name,
                error = albumPlaceholder,
                placeholder = albumPlaceholder,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumCleanCardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.5f),
        ) {
            AlbumCleanCard(
                album = Album(
                    id = "1",
                    name = "Mysterious Climax",
                    artist = "Artist Name",
                    year = 2024,
                    artworkUri = null,
                    trackCount = 27,
                ),
                onClick = { _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumCleanCardSelectedPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.5f),
        ) {
            AlbumCleanCard(
                album = Album(
                    id = "2",
                    name = "Mysterious Climax",
                    artist = "Artist Name",
                    year = 2024,
                    artworkUri = null,
                    trackCount = 27,
                ),
                onClick = { _, _ -> },
                isSelected = true,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
fun AlbumGridCleanPreview() {
    MaterialTheme {
        val sampleAlbums = listOf(
            Album("1", "Mysterious Climax", "Artist 1", 2024, null, 27),
            Album("2", "Mysterious Climax", "Artist 2", 2024, null, 27),
            Album("3", "Mysterious Climax", "Artist 3", 2024, null, 27),
            Album("4", "Mysterious Climax", "Artist 4", 2024, null, 27),
            Album("5", "Abbey Road", "The Beatles", 1969, null, 17),
            Album("6", "Dark Side of the Moon", "Pink Floyd", 1973, null, 10),
        )

        AlbumGridClean(
            albums = sampleAlbums,
            selectedAlbumId = "4",
            onAlbumClick = { _, _, _ -> },
        )
    }
}
