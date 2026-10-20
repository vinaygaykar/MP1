package io.github.vinaygaykar.mp1.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaygaykar.mp1.data.repo.ArtistImageRepository
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.toArtistId
import javax.inject.Inject

@HiltViewModel
class SongArtistsViewModel @Inject constructor(
    private val artistImageRepository: ArtistImageRepository,
) : ViewModel() {

    suspend fun getArtistImageUrl(artistName: String, artistId: String): String? {
        return artistImageRepository.getArtistImageUrl(artistName, artistId)
    }
}

/**
 * Stateful wrapper for SongArtistsDialog that integrates with [SongArtistsViewModel].
 */
@Composable
fun SongArtistsDialog(
    trackTitle: String,
    artistNames: List<String>,
    onDismissRequest: () -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SongArtistsViewModel = hiltViewModel(),
) {
    SongArtistsDialogContent(
        trackTitle = trackTitle,
        artistNames = artistNames,
        onDismissRequest = onDismissRequest,
        onArtistClick = onArtistClick,
        onFetchImageUrl = { artistName, artistId ->
            viewModel.getArtistImageUrl(artistName, artistId)
        },
    )
}

/**
 * Stateless content composable for displaying a list of artists for a track.
 */
@Composable
fun SongArtistsDialogContent(
    trackTitle: String,
    artistNames: List<String>,
    onDismissRequest: () -> Unit,
    onArtistClick: (String) -> Unit,
    onFetchImageUrl: suspend (artistName: String, artistId: String) -> String?,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (trackTitle.isNotBlank()) {
                            Text(
                                text = trackTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Artist List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(artistNames, key = { it }) { name ->
                        SongArtistDialogItem(
                            artistName = name,
                            onClick = { onArtistClick(name) },
                            onFetchImageUrl = onFetchImageUrl,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongArtistDialogItem(
    artistName: String,
    onClick: () -> Unit,
    onFetchImageUrl: suspend (String, String) -> String?,
) {
    val artistId = remember(artistName) { artistName.toArtistId() }
    var imageUrl by remember(artistName) { mutableStateOf<String?>(null) }

    LaunchedEffect(artistName, artistId) {
        val fetchedUrl = onFetchImageUrl(artistName, artistId)
        if (!fetchedUrl.isNullOrEmpty()) {
            imageUrl = fetchedUrl
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongArtistsDialogPreview() {
    MusicPlayerTheme {
        SongArtistsDialogContent(
            trackTitle = "Midnight City",
            artistNames = listOf("M83", "Anthony Gonzalez"),
            onDismissRequest = {},
            onArtistClick = {},
            onFetchImageUrl = { _, _ -> null },
        )
    }
}
