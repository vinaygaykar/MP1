@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.vinaygaykar.mp1.ui.library.tabs

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.components.EmptyState
import io.github.vinaygaykar.mp1.ui.components.artistSharedElement
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.ui.components.StaggeredItemWrapper
import io.github.vinaygaykar.mp1.ui.library.LibraryViewModel
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.ui.theme.isArtistMatchingPlayingTrack
import io.github.vinaygaykar.mp1.ui.theme.rememberMorphingArtistShape
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val EMPTY_STATE_MESSAGE = "No Artists found.\nScan a folder from Settings."

@Composable
fun ArtistGrid(
    artists: List<Artist>,
    onArtistClick: (Artist, String?) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    detailProgress: Float = 1f,
    categoryKey: Any? = null,
    isSplashActive: Boolean = false,
    isCategoryEntranceComplete: Boolean = false,
    libraryViewModel: LibraryViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    playerViewModel: PlayerViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val playingArtist by remember(playerViewModel) {
        playerViewModel?.playbackState?.map { it.artist }?.distinctUntilChanged() ?: flowOf("")
    }.collectAsState(initial = "")
    val playingAlbum by remember(playerViewModel) {
        playerViewModel?.playbackState?.map { it.album }?.distinctUntilChanged() ?: flowOf("")
    }.collectAsState(initial = "")
    val isPlaying by remember(playerViewModel) {
        playerViewModel?.playbackState?.map { it.isPlaying }?.distinctUntilChanged()
            ?: flowOf(false)
    }.collectAsState(initial = false)

    if (artists.isEmpty()) {
        EmptyState(
            message = EMPTY_STATE_MESSAGE,
            modifier = Modifier.padding(contentPadding)
        )
    } else {
        val gridState = rememberLazyGridState()
        val mergedPadding = PaddingValues(
            start = 8.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = mergedPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
                    StaggeredItemWrapper(
                        index = index,
                        categoryKey = categoryKey,
                        detailProgress = detailProgress,
                        isSplashActive = isSplashActive,
                        isCategoryEntranceComplete = isCategoryEntranceComplete
                    ) {
                        ArtistGridItem(
                            artist = artist,
                            playingArtist = playingArtist,
                            playingAlbum = playingAlbum,
                            isPlaying = isPlaying,
                            onClick = { imageUrl -> onArtistClick(artist, imageUrl) },
                            onFetchImageUrl = { name, id ->
                                libraryViewModel?.getArtistImageUrl(
                                    name,
                                    id
                                )
                            }
                        )
                    }
                }
            }
            SimpleScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding()
                    ),
                gridState = gridState
            )
        }
    }
}

@Composable
fun ArtistGridItem(
    artist: Artist,
    onClick: (String?) -> Unit,
    playingArtist: String = "",
    playingAlbum: String = "",
    isPlaying: Boolean = false,
    onFetchImageUrl: suspend (String, String) -> String? = { _, _ -> null }
) {
    var displayImageUrl by remember(artist.imageUrl) { mutableStateOf(artist.imageUrl) }

    LaunchedEffect(artist.id, artist.name, artist.imageUrl) {
        if (displayImageUrl.isNullOrEmpty()) {
            val fetchedUrl = onFetchImageUrl(artist.name, artist.id)
            if (!fetchedUrl.isNullOrEmpty()) {
                displayImageUrl = fetchedUrl
            }
        }
    }

    val isMatchingArtist = remember(artist.name, playingArtist, playingAlbum) {
        isArtistMatchingPlayingTrack(
            artistName = artist.name,
            playingArtistString = playingArtist,
            playingAlbumString = playingAlbum
        )
    }

    val isMorphing = isPlaying && isMatchingArtist

    val artistShape = rememberMorphingArtistShape(
        artistId = artist.id,
        artistName = artist.name,
        isMorphing = isMorphing
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(displayImageUrl) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Artist Image with custom polygon shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .artistSharedElement(key = "artist_image_${artist.id}")
                .clip(artistShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val personPlaceholder = rememberVectorPainter(Icons.Default.Person)
            val gridImageRequest = remember(displayImageUrl) {
                ImageRequest.Builder(context)
                    .data(displayImageUrl)
                    .apply {
                        if (!displayImageUrl.isNullOrEmpty()) {
                            placeholderMemoryCacheKey(displayImageUrl)
                        }
                    }
                    .crossfade(false)
                    .build()
            }

            AsyncImage(
                model = gridImageRequest,
                contentDescription = artist.name,
                error = personPlaceholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private val sampleArtists = listOf(
    Artist(id = "1", name = "M83", trackCount = 12),
    Artist(id = "2", name = "The Weeknd", trackCount = 24),
    Artist(id = "3", name = "Daft Punk", trackCount = 18),
    Artist(id = "4", name = "Coldplay", trackCount = 30)
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
private fun ArtistsTabPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ArtistGrid(
                artists = sampleArtists,
                onArtistClick = { _, _ -> }
            )
        }
    }
}

