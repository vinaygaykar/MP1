package io.github.vinaygaykar.mp1.ui.library.tabs

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp

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
