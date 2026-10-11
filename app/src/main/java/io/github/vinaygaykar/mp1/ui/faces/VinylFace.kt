package io.github.vinaygaykar.mp1.ui.faces

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

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
        val context = LocalContext.current
        val view = LocalView.current

        val displayArtist = remember(state.artist, state.trackTitle) {
            collectArtistNames(state.artist, state.trackTitle).joinArtistsForDisplay()
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AutoResizedText(
                text = state.trackTitle,
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
            if (state.album.isNotBlank()) {
                Text(
                    text = state.album,
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
                            val albumId = state.albumId
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
