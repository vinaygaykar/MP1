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
