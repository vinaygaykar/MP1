package io.github.vinaygaykar.mp1.ui.library.tabs

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun AlbumsTab(
    albums: List<Album>,
    useCleanAlbumGrid: Boolean,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (useCleanAlbumGrid) {
        AlbumGridClean(
            albums = albums,
            onAlbumClick = { album, offset, size ->
                val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                    putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id)
                }
                launchActivityWithScaleUp(
                    context = context,
                    intent = intent,
                    view = view,
                    startX = offset.x.toInt(),
                    startY = offset.y.toInt(),
                    width = size.width,
                    height = size.height
                )
            },
            contentPadding = contentPadding
        )
    } else {
        AlbumGridPunk(
            albums = albums,
            onAlbumClick = { album ->
                val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                    putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id)
                }
                launchActivityWithScaleUp(context = context, intent = intent, view = view)
            },
            contentPadding = contentPadding
        )
    }
}

private val sampleAlbums = listOf(
    Album(id = "1", name = "Hurry Up, We're Dreaming", artist = "M83", year = 2011, artworkUri = null, trackCount = 22),
    Album(id = "2", name = "Starboy", artist = "The Weeknd", year = 2016, artworkUri = null, trackCount = 18),
    Album(id = "3", name = "Random Access Memories", artist = "Daft Punk", year = 2013, artworkUri = null, trackCount = 13),
    Album(id = "4", name = "A Head Full of Dreams", artist = "Coldplay", year = 2015, artworkUri = null, trackCount = 11),
    Album(id = "5", name = "AM", artist = "Arctic Monkeys", year = 2013, artworkUri = null, trackCount = 12),
    Album(id = "6", name = "Currents", artist = "Tame Impala", year = 2015, artworkUri = null, trackCount = 13)
)

@Preview(
    name = "Clean - Portrait Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Clean - Portrait Dark",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Clean - Landscape Light",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Clean - Landscape Dark",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AlbumsTabCleanPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumsTab(
                albums = sampleAlbums,
                useCleanAlbumGrid = true
            )
        }
    }
}

@Preview(
    name = "Punk - Portrait Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Punk - Portrait Dark",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Punk - Landscape Light",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Punk - Landscape Dark",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AlbumsTabPunkPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumsTab(
                albums = sampleAlbums,
                useCleanAlbumGrid = false
            )
        }
    }
}


