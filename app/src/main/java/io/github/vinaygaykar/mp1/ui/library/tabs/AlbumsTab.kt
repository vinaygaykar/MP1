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
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val view = LocalView.current

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

private val sampleAlbums = listOf(
    Album(
        id = "1",
        name = "Abbey Road",
        artist = "The Beatles",
        year = 1969,
        artworkUri = null,
        trackCount = 17
    ),
    Album(
        id = "2",
        name = "Back in Black",
        artist = "AC/DC",
        year = 1980,
        artworkUri = null,
        trackCount = 10
    ),
    Album(
        id = "3",
        name = "Dark Side of the Moon",
        artist = "Pink Floyd",
        year = 1973,
        artworkUri = null,
        trackCount = 10
    ),
    Album(
        id = "4",
        name = "Hotel California",
        artist = "Eagles",
        year = 1976,
        artworkUri = null,
        trackCount = 9
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
private fun AlbumsTabPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumsTab(
                albums = sampleAlbums
            )
        }
    }
}

