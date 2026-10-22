package io.github.vinaygaykar.mp1.ui.nowplaying

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay

@Composable
internal fun NowPlayingTrackInfo(
    modifier: Modifier = Modifier,
    playbackState: PlaybackUiState,
    queueState: QueueUiState = QueueUiState(),
    pagerState: PagerState? = null,
    onArtistClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (pagerState == null) {
        NowPlayingTrackTextContext(
            title = playbackState.trackTitle,
            artist = playbackState.artist,
            album = playbackState.album,
            albumId = playbackState.albumId,
            onArtistClick = onArtistClick,
            context = context,
            view = view,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    } else {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

        val items = queueState.items
        val currentIndex = queueState.currentIndex

        val hasPrevPage = remember(items.size, currentIndex, playbackState.repeatMode) {
            if (items.size <= 1) {
                playbackState.repeatMode == Player.REPEAT_MODE_ALL
            } else {
                playbackState.repeatMode == Player.REPEAT_MODE_ALL || currentIndex > 0
            }
        }

        val hasNextPage = remember(items.size, currentIndex, playbackState.repeatMode) {
            if (items.size <= 1) {
                playbackState.repeatMode == Player.REPEAT_MODE_ALL
            } else {
                playbackState.repeatMode == Player.REPEAT_MODE_ALL || currentIndex in 0 until items.size - 1
            }
        }

        val prevItem = remember(items, currentIndex, hasPrevPage) {
            if (!hasPrevPage || items.isEmpty()) null
            else {
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
                items.getOrNull(prevIndex)
            }
        }

        val nextItem = remember(items, currentIndex, hasNextPage) {
            if (!hasNextPage || items.isEmpty()) null
            else {
                val nextIndex = if (currentIndex in 0 until items.size - 1) currentIndex + 1 else 0
                items.getOrNull(nextIndex)
            }
        }

        val currentTrackPageIndex = if (hasPrevPage) 1 else 0
        val totalPageCount = 1 + (if (hasPrevPage) 1 else 0) + (if (hasNextPage) 1 else 0)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
        ) {
            for (page in 0 until totalPageCount) {
                val isCurrent = page == currentTrackPageIndex
                val isPrev = hasPrevPage && page == currentTrackPageIndex - 1
                val isNext = hasNextPage && page == currentTrackPageIndex + 1

                val item = when {
                    isPrev -> prevItem
                    isNext -> nextItem
                    else -> null
                }

                val title = if (isCurrent) playbackState.trackTitle else item?.mediaMetadata?.title?.toString().orEmpty()
                val artist = if (isCurrent) playbackState.artist else item?.mediaMetadata?.artist?.toString().orEmpty()
                val album = if (isCurrent) playbackState.album else item?.mediaMetadata?.albumTitle?.toString().orEmpty()
                val albumId = if (isCurrent) playbackState.albumId else item?.mediaMetadata?.extras?.getString("album_id")

                if (isCurrent || isPrev || isNext) {
                    NowPlayingTrackTextContext(
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        onArtistClick = if (isCurrent) onArtistClick else null,
                        context = context,
                        view = view,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                val pageOffset =
                                    (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                                translationX = pageOffset * screenWidthPx
                            }
                    )
                }
            }
        }
    }
}


@Composable
private fun NowPlayingTrackTextContext(
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

@Preview(showBackground = true, name = "Now Playing Track Info - Light")
@Composable
fun NowPlayingTrackInfoLightPreview() {
    MusicPlayerTheme(darkTheme = false) {
        Surface {
            NowPlayingTrackInfo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                playbackState = PlaybackUiState(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    album = "Hurry Up, We're Dreaming"
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Now Playing Track Info - Dark")
@Composable
fun NowPlayingTrackInfoDarkPreview() {
    MusicPlayerTheme(darkTheme = true) {
        Surface {
            NowPlayingTrackInfo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                playbackState = PlaybackUiState(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    album = "Hurry Up, We're Dreaming"
                )
            )
        }
    }
}
