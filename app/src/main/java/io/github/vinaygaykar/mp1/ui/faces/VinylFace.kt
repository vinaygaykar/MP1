package io.github.vinaygaykar.mp1.ui.faces

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay

private data class TrackInfo(
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String?
)

class VinylFace : PlayerFace {
    override val id: String = "vinyl"
    override val displayName: String = "Vinyl Record"

    @Composable
    override fun Render(
        state: PlayerUiState,
        actions: PlayerActions,
        modifier: Modifier,
        showRecord: Boolean,
        onArtistClick: (() -> Unit)?
    ) {
        Render(
            state = state,
            actions = actions,
            modifier = modifier,
            showRecord = showRecord,
            pagerState = null,
            onArtistClick = onArtistClick
        )
    }

    @Composable
    override fun Render(
        state: PlayerUiState,
        actions: PlayerActions,
        modifier: Modifier,
        showRecord: Boolean,
        pagerState: PagerState?,
        onArtistClick: (() -> Unit)?
    ) {
        val context = LocalContext.current
        val view = LocalView.current

        if (pagerState == null) {
            TrackTextContent(
                title = state.trackTitle,
                artist = state.artist,
                album = state.album,
                albumId = state.albumId,
                onArtistClick = onArtistClick,
                context = context,
                view = view,
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp)
            )
        } else {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

            val currentTrackInfo = remember(state.trackTitle, state.artist, state.album, state.albumId) {
                TrackInfo(state.trackTitle, state.artist, state.album, state.albumId)
            }

            val prevItem = remember(state.queue, state.currentIndex) {
                if (state.currentIndex > 0) state.queue.getOrNull(state.currentIndex - 1) else null
            }
            val prevTrackInfo = remember(prevItem) {
                prevItem?.let { item ->
                    TrackInfo(
                        title = item.mediaMetadata.title?.toString() ?: "",
                        artist = item.mediaMetadata.artist?.toString() ?: "",
                        album = item.mediaMetadata.albumTitle?.toString() ?: "",
                        albumId = item.mediaMetadata.extras?.getString("album_id")
                    )
                }
            }

            val nextItem = remember(state.queue, state.currentIndex) {
                if (state.currentIndex >= 0) state.queue.getOrNull(state.currentIndex + 1) else null
            }
            val nextTrackInfo = remember(nextItem) {
                nextItem?.let { item ->
                    TrackInfo(
                        title = item.mediaMetadata.title?.toString() ?: "",
                        artist = item.mediaMetadata.artist?.toString() ?: "",
                        album = item.mediaMetadata.albumTitle?.toString() ?: "",
                        albumId = item.mediaMetadata.extras?.getString("album_id")
                    )
                }
            }

            val currentTrackPageIndex = if (state.hasPrevious) 1 else 0
            val totalPageCount = 1 + (if (state.hasPrevious) 1 else 0) + (if (state.hasNext) 1 else 0)

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = 64.dp)
                    .clipToBounds()
            ) {
                for (page in 0 until totalPageCount) {
                    val trackInfo = when {
                        page == currentTrackPageIndex -> currentTrackInfo
                        state.hasPrevious && page == currentTrackPageIndex - 1 -> prevTrackInfo
                        state.hasNext && page == currentTrackPageIndex + 1 -> nextTrackInfo
                        else -> null
                    }

                    if (trackInfo != null) {
                        TrackTextContent(
                            title = trackInfo.title,
                            artist = trackInfo.artist,
                            album = trackInfo.album,
                            albumId = trackInfo.albumId,
                            onArtistClick = if (page == currentTrackPageIndex) onArtistClick else null,
                            context = context,
                            view = view,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .graphicsLayer {
                                    val pageOffset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                                    translationX = pageOffset * screenWidthPx
                                }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TrackTextContent(
        title: String,
        artist: String,
        album: String,
        albumId: String?,
        onArtistClick: (() -> Unit)?,
        context: Context,
        view: View,
        modifier: Modifier = Modifier
    ) {
        val displayArtist = remember(artist, title) {
            collectArtistNames(artist, title).joinArtistsForDisplay()
        }

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.Start,
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                minFontSize = 20.sp,
                maxFontSize = 36.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = displayArtist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .then(
                        if (onArtistClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onArtistClick() }
                        } else Modifier
                    ),
            )
            if (album.isNotBlank()) {
                Text(
                    text = album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable {
                            if (!albumId.isNullOrEmpty()) {
                                val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                                    putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumId)
                                }
                                launchActivityWithScaleUp(context, intent, view)
                            }
                        },
                )
            }
        }
    }
}
