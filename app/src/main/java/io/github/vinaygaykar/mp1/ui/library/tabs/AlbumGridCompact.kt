package io.github.vinaygaykar.mp1.ui.library.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import kotlin.math.abs

/**
 * AlbumGridCompact renders a vertical list of album titles with typographic focus on the centered album.
 *
 * 1. Shows only a list of album titles. The selected album is determined by vertical scroll centering.
 * 2. Non-selected titles fill available width in 1 line. Selected title occupies 60% width up to 3 lines.
 * 3. Selected title has bright font, non-selected titles are muted.
 * 4. Selected album artwork floats on the center-right, occupying 40% of parent width, overlapping non-selected titles.
 * 5. Artwork maintains a 1:1 aspect ratio.
 */
@Composable
fun AlbumGridCompact(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (albums.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lazyListState = rememberLazyListState()

    // Find the item index closest to the vertical center of the viewport
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + (item.size / 2f)
                    abs(itemCenter - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    val selectedAlbum = remember(albums, centerIndex) {
        albums.getOrNull(centerIndex.coerceIn(0, albums.size - 1))
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val halfHeight = this@BoxWithConstraints.maxHeight / 2
        val topPadding = (halfHeight - 40.dp).coerceAtLeast(0.dp)
        val bottomPadding = (halfHeight - 40.dp + contentPadding.calculateBottomPadding()).coerceAtLeast(0.dp)

        // 1. Titles LazyColumn
        LazyColumn(
            state = lazyListState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = bottomPadding,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                val isSelected = index == centerIndex
                CompactAlbumTitleItem(
                    album = album,
                    isSelected = isSelected,
                    onClick = { onAlbumClick(album) }
                )
            }
        }

        // 2. Floating Album Art (40% width, 1:1 ratio, floating center-right over non-selected text)
        selectedAlbum?.let { album ->
            AnimatedContent(
                targetState = album,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "AlbumArtCompactCrossfade",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .fillMaxWidth(0.40f)
                    .aspectRatio(1f)
                    .zIndex(10f)
            ) { targetAlbum ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RectangleShape)
                        .clickable { onAlbumClick(targetAlbum) },
                    shape = RectangleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetAlbum.artworkUri != null) {
                            val albumPlaceholder = rememberVectorPainter(Icons.Default.Album)
                            AsyncImage(
                                model = targetAlbum.artworkUri,
                                contentDescription = targetAlbum.name,
                                error = albumPlaceholder,
                                placeholder = albumPlaceholder,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Scrollbar overlay
        SimpleScrollBar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(100f)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            listState = lazyListState
        )
    }
}

/**
 * Album title text row for Compact view.
 * - Non-selected: 100% width, 1 line, muted font.
 * - Selected: 60% width, up to 3 lines, bright font.
 */
@Composable
fun CompactAlbumTitleItem(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(
                if (isSelected) {
                    Modifier.fillMaxWidth(0.60f)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = album.name,
            style = if (isSelected) {
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            } else {
                MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            },
            color = if (isSelected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f)
            },
            maxLines = if (isSelected) 3 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
fun AlbumGridCompactPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val sampleAlbums = listOf(
                Album("1", "Lorem Ipsum Dolor Sit Amet Consectetur Adipiscing Elit", "Artist 1", 2024, null, 12),
                Album("2", "Sed Do Eiusmod Tempor Incididunt Ut Labore Et Dolore Magna Aliqua", "Artist 2", 2024, null, 10),
                Album("3", "Ut Enim Ad Minim Veniam Quis Nostrud Exercitation Ullamco Laboris Nisi Ut Aliquip Ex Ea Commodo Consequat", "Artist 3", 2024, null, 8),
                Album("4", "Duis Aute Irure Dolor In Reprehenderit In Voluptate Velit Esse Cillum Dolore Eu Fugiat Nulla Pariatur", "Artist 4", 2024, null, 15),
                Album("5", "Excepteur Sint Occaecat Cupidatat Non Proident Sunt In Culpa Qui Officia Deserunt Mollit Anim Id Est Laborum", "Artist 5", 2023, null, 11),
                Album("6", "Curabitur Pretium Tincidunt Lacus Nulla Gravida Orci A Odio Nullam Varius", "Artist 6", 2023, null, 9),
                Album("7", "Fusce Feugiat Malesuada Odio Morbi Nunc Odio Gravida At Cursus Nec Luctus A Lorem", "Artist 7", 2023, null, 14),
                Album("8", "Maecenas Tristique Orci Ac Sem Duis Ultricies Pharetra Magna Donec Accumsan Malesuada Orci", "Artist 8", 2022, null, 10),
                Album("9", "Donec Sit Amet Eros Lorem Ipsum Dolor Sit Amet Consectetur Adipiscing Elit", "Artist 9", 2022, null, 7)
            )

            AlbumGridCompact(
                albums = sampleAlbums,
                onAlbumClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompactAlbumTitleItemPreview() {
    MaterialTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                CompactAlbumTitleItem(
                    album = Album(
                        "1",
                        "Lorem Ipsum Dolor Sit Amet Consectetur Adipiscing Elit Sed Do Eiusmod Tempor Incididunt Ut Labore Et Dolore Magna Aliqua",
                        "Artist",
                        2024,
                        null,
                        10
                    ),
                    isSelected = true,
                    onClick = {}
                )
            }
        }
    }
}
