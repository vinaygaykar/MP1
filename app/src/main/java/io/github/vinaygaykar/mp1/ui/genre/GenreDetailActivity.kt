package io.github.vinaygaykar.mp1.ui.genre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.album.SongCardItem
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.library.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.ExpandablePlayer
import io.github.vinaygaykar.mp1.ui.library.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.library.MultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.PlaylistSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.library.SelectionCountPill
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenreDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_GENRE_ID = "extra_genre_id"
        const val EXTRA_GENRE_NAME = "extra_genre_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                GenreDetailScreen(onBack = { finish() })
            }
        }
    }
}

fun getDeterministicVibrantSeedColor(title: String): Color {
    if (title.isBlank()) return Color(0xFF6200EE)
    val hash = title.hashCode()
    val positiveHash = hash.toLong() and 0x7FFFFFFF
    val hue = (positiveHash % 360).toFloat()
    val saturation = 0.85f
    val lightness = 0.50f
    val argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    return Color(argb)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    onBack: () -> Unit,
    viewModel: GenreDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val genre by viewModel.genre.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val visualizerState by playerViewModel.visualizerState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showPlaylistSelectionSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistForSelectionDialog by remember { mutableStateOf(false) }

    val selectedSongs = remember(songs, selectedSongIds) {
        songs.filter { selectedSongIds.contains(it.id) }
    }

    fun toggleSongSelection(song: Song) {
        selectedSongIds = if (selectedSongIds.contains(song.id)) {
            selectedSongIds - song.id
        } else {
            selectedSongIds + song.id
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedSongIds = emptySet()
    }

    val isDark = isSystemInDarkTheme()

    val genreTitle = genre?.name ?: viewModel.initialGenreName
    val displayGenreTitle = if (genreTitle.isBlank()) "Genre" else genreTitle

    val seedColor = remember(displayGenreTitle) {
        getDeterministicVibrantSeedColor(displayGenreTitle)
    }

    val themeColorScheme: ColorScheme = remember(seedColor, isDark) {
        DynamicThemeHelper.createDynamicColorScheme(seedColor, isDark = isDark)
    }

    MaterialTheme(colorScheme = themeColorScheme) {
        val isPlayerActive = playerState.trackTitle.isNotEmpty()
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

        val currentPlayingMediaId = remember(playerState.queue, playerState.currentIndex) {
            if (playerState.currentIndex in playerState.queue.indices) {
                playerState.queue[playerState.currentIndex].mediaId
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

            if (showCreatePlaylistForSelectionDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistForSelectionDialog = false },
                    onCreate = { name ->
                        viewModel.createPlaylistAndAddSongs(name, selectedSongs)
                        selectedSongIds = emptySet()
                    }
                )
            }

            Scaffold(
                topBar = {
                    if (isSelectionMode) {
                        SelectionActionRow(
                            selectedCount = selectedSongIds.size,
                            onSelectAll = { selectedSongIds = songs.map { it.id }.toSet() },
                            onDeselect = { selectedSongIds = emptySet() },
                            onOptionsClick = { showMultiSelectionSheet = true },
                            modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        )
                    } else {
                        TopAppBar(
                            title = {
                                AnimatedVisibility(
                                    visible = showTopBarTitle,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text = displayGenreTitle,
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
                    // 1. Genre Name in Big Bold Font (max 3 lines)
                    item {
                        AutoResizedText(
                            text = displayGenreTitle,
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

                    // 2. Track Count and Playback Controls Row
                    item {
                        val trackCount = songs.size.takeIf { it > 0 } ?: genre?.trackCount ?: 0
                        val tracksText = if (trackCount == 1) "1 track" else "$trackCount tracks"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tracksText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FilledTonalIconButton(
                                onClick = { viewModel.playGenre(shuffle = true) },
                                enabled = songs.isNotEmpty(),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
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

                    // 3. List of Songs for that Genre
                    if (songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No songs found for this genre.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            val isCurrentSong = (currentPlayingMediaId == song.id) ||
                                    (currentPlayingMediaId.isNullOrEmpty() && playerState.trackTitle == song.title)
                            SongCardItem(
                                song = song,
                                trackIndex = index + 1,
                                isCurrentSong = isCurrentSong,
                                onClick = {
                                    if (isSelectionMode) {
                                        toggleSongSelection(song)
                                    } else {
                                        viewModel.playSong(song)
                                    }
                                },
                                onAddToQueue = { offset, size -> handleAddToQueue(song, offset, size) },
                                isSelected = selectedSongIds.contains(song.id),
                                isSelectionMode = isSelectionMode,
                                onLongClick = { toggleSongSelection(song) }
                            )
                        }
                    }
                }
            }

            // Expandable Player
            ExpandablePlayer(
                state = playerState,
                actions = playerViewModel,
                visualizerState = visualizerState,
                onProgressUpdate = { expansionProgress.floatValue = it },
                peekHeight = 72.dp,
                onMiniPlayerPositioned = { miniPlayerTargetOffset = it },
                modifier = Modifier.fillMaxSize()
            )

            // Multi-selection Bottom Sheet
            if (showMultiSelectionSheet) {
                MultiSelectionBottomSheet(
                    selectedSongs = selectedSongs,
                    onDismiss = { showMultiSelectionSheet = false },
                    onPlayAll = {
                        if (selectedSongs.isNotEmpty()) {
                            viewModel.playSong(selectedSongs.first())
                            selectedSongs.drop(1).forEach { viewModel.addToQueue(it) }
                            selectedSongIds = emptySet()
                        }
                    },
                    onAddToPlaylist = {
                        showMultiSelectionSheet = false
                        showPlaylistSelectionSheet = true
                    },
                    onAddToQueue = {
                        selectedSongs.forEach { viewModel.addToQueue(it) }
                        selectedSongIds = emptySet()
                    }
                )
            }

            // Playlist Selection Bottom Sheet
            if (showPlaylistSelectionSheet) {
                PlaylistSelectionBottomSheet(
                    playlists = playlists,
                    onDismiss = { showPlaylistSelectionSheet = false },
                    onSelectPlaylist = { playlist ->
                        viewModel.addSongsToPlaylist(playlist.id, selectedSongs)
                        selectedSongIds = emptySet()
                    },
                    onCreateNewPlaylist = {
                        showPlaylistSelectionSheet = false
                        showCreatePlaylistForSelectionDialog = true
                    }
                )
            }
        }
    }
}
