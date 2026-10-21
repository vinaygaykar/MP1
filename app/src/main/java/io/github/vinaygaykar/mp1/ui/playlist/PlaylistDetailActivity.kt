package io.github.vinaygaykar.mp1.ui.playlist

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.genre.getDeterministicVibrantSeedColor
import io.github.vinaygaykar.mp1.ui.nowplaying.NowPlayingScreen
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.ceil

@AndroidEntryPoint
class PlaylistDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_PLAYLIST_NAME = "extra_playlist_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                PlaylistDetailScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val progressState by playerViewModel.progressState.collectAsState()
    val queueState by playerViewModel.queueState.collectAsState()
    val lyricsState by playerViewModel.lyricsState.collectAsState()
    val visualizerState by playerViewModel.visualizerState.collectAsState()

    val isDark = isSystemInDarkTheme()

    val playlistTitle = playlist?.name ?: viewModel.initialPlaylistName
    val displayPlaylistTitle = if (playlistTitle.isBlank()) "Playlist" else playlistTitle

    val seedColor = remember(displayPlaylistTitle) {
        getDeterministicVibrantSeedColor(displayPlaylistTitle)
    }

    val themeColorScheme: ColorScheme = remember(seedColor, isDark) {
        DynamicThemeHelper.createDynamicColorScheme(seedColor, isDark = isDark)
    }

    MaterialTheme(colorScheme = themeColorScheme) {
        val isPlayerActive = playbackState.trackTitle.isNotEmpty()
        val expansionProgress = remember { mutableFloatStateOf(0f) }
        val lazyListState = rememberLazyListState()

        val showTopBarTitle by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex > 1 ||
                        (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset > 40)
            }
        }

        val topBarContainerColor = if (showTopBarTitle) {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        } else {
            Color.Transparent
        }

        val currentPlayingMediaId = remember(queueState.items, queueState.currentIndex) {
            if (queueState.currentIndex in queueState.items.indices) {
                queueState.items[queueState.currentIndex].mediaId
            } else null
        }

        var miniPlayerTargetOffset by remember { mutableStateOf<Offset?>(null) }
        val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
        var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }
        var rootSize by remember { mutableStateOf(IntSize.Zero) }

        val handleAddToQueue: (Song, Offset, IntSize) -> Unit = { song, offset, size ->
            viewModel.addToQueue(song)
            if (isPlayerActive) {
                flyingArtworks.add(
                    FlyingArtworkItem(
                        artworkUri = song.artworkUri,
                        startOffset = offset,
                        startSize = size
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    rootOffsetInWindow = coords.positionInRoot()
                    rootSize = coords.size
                }
        ) {
            val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPadding = if (isPlayerActive) 72.dp + navBarsPadding else navBarsPadding + 16.dp

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            AnimatedVisibility(
                                visible = showTopBarTitle,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    text = displayPlaylistTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        },
                        navigationIcon = {
                            FilledTonalIconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarContainerColor
                        )
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Playlist Name
                    item {
                        AutoResizedText(
                            text = displayPlaylistTitle,
                            style = MaterialTheme.typography.displayMedium,
                            maxLines = 3,
                            minFontSize = 24.sp,
                            maxFontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    // 2. Track Count, Total Duration, and Shuffle Control
                    item {
                        val trackCount = songs.size
                        val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
                        val totalMinutes = remember(totalDurationMs) { ceil(totalDurationMs / 60000.0).toInt() }

                        val songsText = if (trackCount == 1) "1 song" else "$trackCount songs"
                        val durationText = if (totalMinutes == 1) "1 min" else "$totalMinutes mins"
                        val infoText = if (trackCount > 0) "$songsText • $durationText" else "$songsText • 0 mins"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = infoText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FilledTonalIconButton(
                                onClick = { viewModel.playPlaylist(shuffle = true) },
                                enabled = songs.isNotEmpty(),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle"
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 3. Songs List or Empty State Description
                    if (songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No songs in this playlist yet.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "You can add songs to your playlist from the Songs tab in your library.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(songs, key = { it.id }) { song ->
                            val isCurrentSong = (currentPlayingMediaId == song.id) ||
                                    (currentPlayingMediaId.isNullOrEmpty() && playbackState.trackTitle == song.title)
                            PlaylistSongCardItem(
                                song = song,
                                isCurrentSong = isCurrentSong,
                                onClick = { viewModel.playSong(song) },
                                onAddToQueue = { offset, size -> handleAddToQueue(song, offset, size) }
                            )
                        }
                    }
                }
            }

            // Now Playing Screen
            NowPlayingScreen(
                playbackState = playbackState,
                progressState = progressState,
                queueState = queueState,
                lyricsState = lyricsState,
                playbackControls = playerViewModel,
                queueControls = playerViewModel,
                lyricsControls = playerViewModel,
                visualizerState = visualizerState,
                onProgressUpdate = { expansionProgress.floatValue = it },
                peekHeight = 72.dp,
                onMiniPlayerPositioned = { miniPlayerTargetOffset = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PlaylistSongCardItem(
    song: Song,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onAddToQueue: ((Offset, IntSize) -> Unit)?,
    modifier: Modifier = Modifier,
    cardColor: Color = Color.Transparent
) {
    val containerColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        cardColor
    }

    val titleColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val artistColor = if (isCurrentSong) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val itemShape = if (isCurrentSong) {
        RoundedCornerShape(50.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val view = LocalView.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .clickable { onClick() },
        shape = itemShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .onGloballyPositioned { coords ->
                        itemCoordinates = coords
                    },
                contentAlignment = Alignment.Center
            ) {
                if (song.artworkUri != null) {
                    AsyncImage(
                        model = song.artworkUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song name and artist
            Column(modifier = Modifier.weight(1f)) {
                val displayArtist = remember(song.artistsDisplayString, song.artist) {
                    song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
                }

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onAddToQueue != null) {
                IconButton(
                    onClick = {
                        val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                        val size = itemCoordinates?.size ?: IntSize(100, 100)
                        onAddToQueue(offset, size)
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to Queue",
                        tint = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
