package io.github.vinaygaykar.mp1.ui.library.tabs

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.components.EmptyState
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.ui.components.SongListItem
import io.github.vinaygaykar.mp1.ui.components.StaggeredItemWrapper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

private const val EMPTY_STATE_MESSAGE = "No Songs found.\nScan a folder from Settings."

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    selectedSongIds: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onSongLongClick: (Song) -> Unit = {},
    onSongMoreClick: ((Song, Offset, IntSize) -> Unit)? = null,
    headerHeight: Dp = 104.dp,
    detailProgress: Float = 1f,
    categoryKey: Any? = null,
    isSplashActive: Boolean = false,
    isCategoryEntranceComplete: Boolean = false
) {
    if (songs.isEmpty()) {
        val topPadding = contentPadding.calculateTopPadding()
        val bottomPadding = contentPadding.calculateBottomPadding()
        EmptyState(
            message = EMPTY_STATE_MESSAGE,
            modifier = Modifier
                .padding(
                    top = topPadding,
                    bottom = bottomPadding + headerHeight + topPadding
                )
        )
    } else {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    val onClickLambda = remember(song.id, isSelectionMode) {
                        { onSongClick(song) }
                    }
                    val onLongClickLambda = remember(song.id) {
                        { onSongLongClick(song) }
                    }
                    StaggeredItemWrapper(
                        index = index,
                        categoryKey = categoryKey,
                        detailProgress = detailProgress,
                        isSplashActive = isSplashActive,
                        isCategoryEntranceComplete = isCategoryEntranceComplete
                    ) {
                        SongListItem(
                            song = song,
                            onClick = onClickLambda,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onLongClick = onLongClickLambda,
                            onMoreClick = { offset, size ->
                                onSongMoreClick?.invoke(song, offset, size)
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
                listState = listState
            )
        }
    }
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
                onSongClick = {}
            )
        }
    }
}
