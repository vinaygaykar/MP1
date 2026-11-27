package io.github.vinaygaykar.mp1.ui.album

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.artist.CollaboratorArtistCard
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.components.SongHeroOverlayContainer
import io.github.vinaygaykar.mp1.ui.components.SongListItem
import io.github.vinaygaykar.mp1.ui.components.SongOverlayData
import io.github.vinaygaykar.mp1.ui.library.SongsMultiSelectOverlay
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import io.github.vinaygaykar.mp1.utils.splitArtistsByDelimiters
import kotlin.math.ceil


@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onHideNowPlayingView: () -> Unit = {},
    onShowNowPlayingView: () -> Unit = {},
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    val primaryArtist by viewModel.primaryArtist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val featuredArtists by viewModel.featuredArtists.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    AlbumDetailContent(
        album = album,
        primaryArtist = primaryArtist,
        songs = songs,
        featuredArtists = featuredArtists,
        playbackState = playbackState,
        playlists = playlists,
        onBack = onBack,
        onArtistClick = onArtistClick,
        onHideNowPlayingView = onHideNowPlayingView,
        onShowNowPlayingView = onShowNowPlayingView,
        onMiniPlayerCollapseChange = onMiniPlayerCollapseChange,
        onPlaySong = { viewModel.playSong(it) },
        onPlayNext = { viewModel.playNext(it) },
        onAddSongToQueue = { viewModel.addToQueue(it) },
        onAddSongsToQueue = { viewModel.addToQueue(it) },
        onAddSongsToPlaylist = { playlistId, songList -> viewModel.addSongsToPlaylist(playlistId, songList) },
        onCreatePlaylistAndAddSongs = { name, songList -> viewModel.createPlaylistAndAddSongs(name, songList) }
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AlbumDetailContent(
    album: Album?,
    primaryArtist: Artist?,
    songs: List<Song>,
    featuredArtists: List<Artist>,
    playbackState: PlaybackUiState,
    playlists: List<Playlist>,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onHideNowPlayingView: () -> Unit = {},
    onShowNowPlayingView: () -> Unit = {},
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    onPlayNext: (List<Song>) -> Unit = {},
    onAddSongToQueue: (Song) -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
    onAddSongsToPlaylist: (Long, List<Song>) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddSongs: (String, List<Song>) -> Unit = { _, _ -> },
    initialSelectedSongIds: Set<String> = emptySet(),
    initialShowMultiSelectionSheet: Boolean = false
) {
    val hazeState = remember { HazeState() }
    var activeOverlayData by remember { mutableStateOf<SongOverlayData?>(null) }
    var selectedSongIds by remember(initialSelectedSongIds) { mutableStateOf(initialSelectedSongIds) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
    var showMultiSelectionSheet by remember(initialShowMultiSelectionSheet) { mutableStateOf(initialShowMultiSelectionSheet) }
    var showPlaylistSelectionSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistForSelectionDialog by remember { mutableStateOf(false) }

    val songsMap = remember(songs) { songs.associateBy { it.id } }
    val selectedSongsInSelectionOrder = remember(songsMap, selectedSongIds) {
        selectedSongIds.mapNotNull { songsMap[it] }
    }

    val isActionPopupOpen = isSelectionMode && showMultiSelectionSheet

    // Back Handler Level 1: When expanded, back press collapses the overlay back to compact mode
    BackHandler(enabled = isActionPopupOpen) {
        showMultiSelectionSheet = false
    }

    // Back Handler Level 2: When collapsed in selection mode, back press exits selection mode and deselects all
    BackHandler(enabled = isSelectionMode && !showMultiSelectionSheet) {
        selectedSongIds = emptySet()
    }

    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            showMultiSelectionSheet = false
        }
    }

    LaunchedEffect(activeOverlayData != null, isActionPopupOpen) {
        if (activeOverlayData != null || isActionPopupOpen) {
            onHideNowPlayingView()
        } else {
            onShowNowPlayingView()
        }
    }

    fun toggleSongSelection(song: Song) {
        selectedSongIds = if (selectedSongIds.contains(song.id)) {
            selectedSongIds - song.id
        } else {
            selectedSongIds + song.id
        }
    }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var seedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(album?.artworkUri) {
        seedColor = DynamicThemeHelper.extractSeedColor(context, album?.artworkUri)
    }

    val themeColorScheme: ColorScheme? = remember(seedColor, isDark) {
        seedColor?.let { color ->
            DynamicThemeHelper.createDynamicColorScheme(color, isDark = isDark)
        }
    }

    val currentColorScheme = themeColorScheme ?: MaterialTheme.colorScheme

    val activity = context as? Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }

    val widthClass = windowSizeClass?.widthSizeClass
    val heightClass = windowSizeClass?.heightSizeClass

    val isWideWindow = if (widthClass != null) {
        widthClass != WindowWidthSizeClass.Compact
    } else {
        val configuration = LocalConfiguration.current
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val isCompactHeight = if (heightClass != null) {
        heightClass == WindowHeightSizeClass.Compact
    } else {
        val configuration = LocalConfiguration.current
        configuration.screenHeightDp < 500
    }

    MaterialTheme(colorScheme = currentColorScheme) {
        val isPlayerActive = playbackState.trackTitle.isNotEmpty()

        val lazyListState = rememberLazyListState()

        val artistName = remember(album?.artist) {
            album?.artist?.splitArtistsByDelimiters()?.joinArtistsForDisplay() ?: album?.artist
        }
        val hasArtist = !artistName.isNullOrBlank() && artistName != "Unknown Artist"

        val songsByDisc = remember(songs) {
            songs.groupBy { it.discNumber ?: 1 }
        }
        val hasMultipleDiscs = songsByDisc.size > 1

        var miniPlayerBulgeTrigger by remember { mutableLongStateOf(0L) }
        var miniPlayerTargetOffset by remember { mutableStateOf<Offset?>(null) }

        LaunchedEffect(playbackState.trackTitle) {
            onMiniPlayerCollapseChange(false)
        }

        LaunchedEffect(playbackState.isPlaying) {
            onMiniPlayerCollapseChange(false)
        }

        val nestedScrollConnection = remember(onMiniPlayerCollapseChange) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y
                    if (deltaY < -8f) {
                        onMiniPlayerCollapseChange(true)
                    } else if (deltaY > 8f) {
                        onMiniPlayerCollapseChange(false)
                    }
                    return Offset.Zero
                }
            }
        }

        val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
        var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }
        var rootSize by remember { mutableStateOf(IntSize.Zero) }

        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .onGloballyPositioned { coords ->
                    rootOffsetInWindow = coords.positionInRoot()
                    rootSize = coords.size
                }
        ) {
            val targetSizePx = with(density) { 56.dp.toPx() }
            val peekHeightPx = with(density) { 72.dp.toPx() }

            val targetX = if (rootSize.width > 0) {
                (rootSize.width.toFloat() / 2f) - (targetSizePx / 2f)
            } else {
                val displayMetrics = context.resources.displayMetrics
                (displayMetrics.widthPixels.toFloat() / 2f) - (targetSizePx / 2f)
            }

            val currentMiniPlayerTargetOffset = miniPlayerTargetOffset
            val targetY = if (currentMiniPlayerTargetOffset != null) {
                (currentMiniPlayerTargetOffset.y - rootOffsetInWindow.y) + (peekHeightPx / 2f) - (targetSizePx / 2f)
            } else if (rootSize.height > 0) {
                rootSize.height.toFloat() - (peekHeightPx / 2f) - (targetSizePx / 2f)
            } else {
                val displayMetrics = context.resources.displayMetrics
                displayMetrics.heightPixels.toFloat() - (peekHeightPx / 2f) - (targetSizePx / 2f)
            }

            val targetLocalOffset = Offset(targetX, targetY)

            flyingArtworks.forEach { item ->
                key(item.id) {
                    FlyingArtworkOverlay(
                        item = item,
                        rootOffset = rootOffsetInWindow,
                        targetOffsetInLocal = targetLocalOffset,
                        onAnimationComplete = {
                            miniPlayerBulgeTrigger = System.currentTimeMillis()
                            flyingArtworks.remove(item)
                        }
                    )
                }
            }

            val navBarBottom =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomPadding =
                if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

            if (showCreatePlaylistForSelectionDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistForSelectionDialog = false },
                    onCreate = { name ->
                        onCreatePlaylistAndAddSongs(name, selectedSongsInSelectionOrder)
                        selectedSongIds = emptySet()
                    }
                )
            }

            SongHeroOverlayContainer(
                modifier = Modifier.fillMaxSize(),
                overlayData = activeOverlayData,
                onDismiss = { activeOverlayData = null },
                onPlayNext = { song -> onPlayNext(listOf(song)) },
                onAddToQueue = { song -> onAddSongToQueue(song) },
                onAddToPlaylist = { song ->
                    selectedSongIds = setOf(song.id)
                    showPlaylistSelectionSheet = true
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    key(seedColor) {
                        when {
                            isWideWindow -> {
                                AlbumDetailLandscapeContent(
                                    album = album,
                                    primaryArtist = primaryArtist,
                                    artistName = artistName,
                                    hasArtist = hasArtist,
                                    songs = songs,
                                    songsByDisc = songsByDisc,
                                    hasMultipleDiscs = hasMultipleDiscs,
                                    featuredArtists = featuredArtists,
                                    playbackState = playbackState,
                                    lazyListState = lazyListState,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    onBack = onBack,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    toggleSongSelection = ::toggleSongSelection,
                                    onMoreClick = { offset, size, song ->
                                        activeOverlayData = SongOverlayData(song, offset, size)
                                    },
                                    bottomPadding = bottomPadding,
                                    statusBarTop = statusBarTop
                                )
                            }
                            isCompactHeight -> {
                                AlbumDetailCompactContent(
                                    album = album,
                                    primaryArtist = primaryArtist,
                                    artistName = artistName,
                                    hasArtist = hasArtist,
                                    songs = songs,
                                    songsByDisc = songsByDisc,
                                    hasMultipleDiscs = hasMultipleDiscs,
                                    featuredArtists = featuredArtists,
                                    playbackState = playbackState,
                                    lazyListState = lazyListState,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    toggleSongSelection = ::toggleSongSelection,
                                    onMoreClick = { offset, size, song ->
                                        activeOverlayData = SongOverlayData(song, offset, size)
                                    },
                                    bottomPadding = bottomPadding,
                                    statusBarTop = statusBarTop
                                )
                            }
                            else -> {
                                AlbumDetailPortraitContent(
                                    album = album,
                                    primaryArtist = primaryArtist,
                                    artistName = artistName,
                                    hasArtist = hasArtist,
                                    songs = songs,
                                    songsByDisc = songsByDisc,
                                    hasMultipleDiscs = hasMultipleDiscs,
                                    featuredArtists = featuredArtists,
                                    playbackState = playbackState,
                                    lazyListState = lazyListState,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    toggleSongSelection = ::toggleSongSelection,
                                    onMoreClick = { offset, size, song ->
                                        activeOverlayData = SongOverlayData(song, offset, size)
                                    },
                                    bottomPadding = bottomPadding,
                                    statusBarTop = statusBarTop
                                )
                            }
                        }
                    }

                    // Floating Back Arrow
                    if (!isActionPopupOpen && !isWideWindow) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .displayCutoutPadding()
                                .padding(start = 16.dp, top = 8.dp)
                                .zIndex(3f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }

                    SongsMultiSelectOverlay(
                        songs = selectedSongsInSelectionOrder,
                        visible = isSelectionMode,
                        isExpanded = isActionPopupOpen,
                        onClick = {
                            showMultiSelectionSheet = !showMultiSelectionSheet
                        },
                        onDismiss = {
                            showMultiSelectionSheet = false
                        },
                        onPlayNext = { activeSongs ->
                            if (activeSongs.isNotEmpty()) {
                                onPlayNext(activeSongs)
                                selectedSongIds = emptySet()
                            }
                        },
                        onAddToPlaylist = { _ ->
                            showMultiSelectionSheet = false
                            showPlaylistSelectionSheet = true
                        },
                        onAddToQueue = { activeSongs ->
                            if (activeSongs.isNotEmpty()) {
                                onAddSongsToQueue(activeSongs)
                                selectedSongIds = emptySet()
                            }
                        },
                        hazeState = hazeState,
                        bottomPadding = bottomPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .zIndex(5f)
                    )
                }
            }

            // Playlist Selection Bottom Sheet
            if (showPlaylistSelectionSheet) {
                PlaylistPickerBottomSheet(
                    playlists = playlists,
                    onDismiss = { showPlaylistSelectionSheet = false },
                    onSelectPlaylist = { playlist ->
                        onAddSongsToPlaylist(playlist.id, selectedSongsInSelectionOrder)
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

@Composable
private fun AlbumDetailPortraitContent(
    album: Album?,
    primaryArtist: Artist?,
    artistName: String?,
    hasArtist: Boolean,
    songs: List<Song>,
    songsByDisc: Map<Int, List<Song>>,
    hasMultipleDiscs: Boolean,
    featuredArtists: List<Artist>,
    playbackState: PlaybackUiState,
    lazyListState: LazyListState,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    onArtistClick: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    toggleSongSelection: (Song) -> Unit,
    onMoreClick: (Offset, IntSize, Song) -> Unit,
    bottomPadding: Dp,
    statusBarTop: Dp
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
        contentPadding = PaddingValues(
            top = statusBarTop + 52.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Album Artwork
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtworkView(
                    album = album,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .widthIn(max = 260.dp)
                        .aspectRatio(1f)
                )
            }
        }

        // 2. Album Name and Artist
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AutoResizedText(
                    text = album?.name ?: "Loading Album...",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    minFontSize = 18.sp,
                    maxFontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (artistName != null && hasArtist) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { primaryArtist?.let { onArtistClick(it.id) } }
                            .basicMarquee()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Details summary (Year • Tracks • Duration) before song list
        item {
            AlbumDetailsSummary(
                album = album,
                songs = songs,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                textAlign = TextAlign.Start
            )
        }

        // 3. List of Songs
        albumSongsListItems(
            songsByDisc = songsByDisc,
            hasMultipleDiscs = hasMultipleDiscs,
            songs = songs,
            selectedSongIds = selectedSongIds,
            isSelectionMode = isSelectionMode,
            onPlaySong = onPlaySong,
            toggleSongSelection = toggleSongSelection,
            onMoreClick = onMoreClick
        )

        // Featured Artists Section
        if (featuredArtists.isNotEmpty()) {
            item {
                FeaturedArtistsSection(
                    featuredArtists = featuredArtists,
                    playbackState = playbackState,
                    onArtistClick = onArtistClick
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailLandscapeContent(
    album: Album?,
    primaryArtist: Artist?,
    artistName: String?,
    hasArtist: Boolean,
    songs: List<Song>,
    songsByDisc: Map<Int, List<Song>>,
    hasMultipleDiscs: Boolean,
    featuredArtists: List<Artist>,
    playbackState: PlaybackUiState,
    lazyListState: LazyListState,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    toggleSongSelection: (Song) -> Unit,
    onMoreClick: (Offset, IntSize, Song) -> Unit,
    bottomPadding: Dp,
    statusBarTop: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .hazeSource(hazeState)
    ) {
        // Pane 1 (Very Left): Dedicated non-scrollable pane with Back Button
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }

        // Pane 2 (Middle): Artwork & Details Summary
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.35f)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = statusBarTop + 12.dp,
                    bottom = bottomPadding,
                    start = 8.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album Artwork
            AlbumArtworkView(
                album = album,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 240.dp)
                    .aspectRatio(1f)
            )

            // Year & Track / Duration Summary (Single Row separated by dots)
            AlbumDetailsSummary(
                album = album,
                songs = songs,
                textAlign = TextAlign.Center
            )
        }

        // Pane 3 (Right): Album Name, Artist Name, Songs List & Featured Artists
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarTop + 12.dp,
                    bottom = bottomPadding,
                    start = 8.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header item: Album Name & Artist Name
                item(key = "album_header_text") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AutoResizedText(
                            text = album?.name ?: "Loading Album...",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            minFontSize = 18.sp,
                            maxFontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (artistName != null && hasArtist) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { primaryArtist?.let { onArtistClick(it.id) } }
                                    .basicMarquee()
                            )
                        }
                    }
                }

                // Songs List
                albumSongsListItems(
                    songsByDisc = songsByDisc,
                    hasMultipleDiscs = hasMultipleDiscs,
                    songs = songs,
                    selectedSongIds = selectedSongIds,
                    isSelectionMode = isSelectionMode,
                    onPlaySong = onPlaySong,
                    toggleSongSelection = toggleSongSelection,
                    onMoreClick = onMoreClick
                )

                // Featured Artists Section all the way at the end
                if (featuredArtists.isNotEmpty()) {
                    item(key = "featured_artists_section_last") {
                        FeaturedArtistsSection(
                            featuredArtists = featuredArtists,
                            playbackState = playbackState,
                            onArtistClick = onArtistClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailCompactContent(
    album: Album?,
    primaryArtist: Artist?,
    artistName: String?,
    hasArtist: Boolean,
    songs: List<Song>,
    songsByDisc: Map<Int, List<Song>>,
    hasMultipleDiscs: Boolean,
    featuredArtists: List<Artist>,
    playbackState: PlaybackUiState,
    lazyListState: LazyListState,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    onArtistClick: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    toggleSongSelection: (Song) -> Unit,
    onMoreClick: (Offset, IntSize, Song) -> Unit,
    bottomPadding: Dp,
    statusBarTop: Dp
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(hazeState),
        contentPadding = PaddingValues(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Header with Album Artwork Background + slight transparent black overlay (Square Shaped)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (album?.artworkUri != null) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.name,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                // Transparent black overlay for text readability
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(
                            top = statusBarTop + 16.dp,
                            bottom = 20.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AutoResizedText(
                        text = album?.name ?: "Loading Album...",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        minFontSize = 18.sp,
                        maxFontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (artistName != null && hasArtist) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable { primaryArtist?.let { onArtistClick(it.id) } }
                                .basicMarquee()
                        )
                    }
                }
            }
        }

        // Album detail summary outside of image, above list, left aligned
        item {
            AlbumDetailsSummary(
                album = album,
                songs = songs,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Start
            )
        }

        // Songs List starting on actual normal background
        albumSongsListItems(
            songsByDisc = songsByDisc,
            hasMultipleDiscs = hasMultipleDiscs,
            songs = songs,
            selectedSongIds = selectedSongIds,
            isSelectionMode = isSelectionMode,
            onPlaySong = onPlaySong,
            toggleSongSelection = toggleSongSelection,
            onMoreClick = onMoreClick
        )

        // Featured Artists
        if (featuredArtists.isNotEmpty()) {
            item {
                FeaturedArtistsSection(
                    featuredArtists = featuredArtists,
                    playbackState = playbackState,
                    onArtistClick = onArtistClick
                )
            }
        }
    }
}

@Composable
private fun AlbumArtworkView(
    album: Album?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkUri = album?.artworkUri
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (artworkUri != null) {
            val request = remember(artworkUri) {
                ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(600, 600)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AlbumDetailsSummary(
    album: Album?,
    songs: List<Song>,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    textAlign: TextAlign = TextAlign.Start
) {
    val yearText = remember(album?.year) { album?.year?.takeIf { it > 0 }?.toString() }
    val trackCount = songs.size
    val tracksText = remember(trackCount) { if (trackCount == 1) "1 track" else "$trackCount tracks" }
    val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
    val durationText = remember(totalDurationMs) { formatAlbumTotalTime(totalDurationMs) }

    val fullSummary = remember(yearText, tracksText, durationText) {
        listOfNotNull(yearText, tracksText, durationText.takeIf { it.isNotEmpty() })
            .joinToString(separator = " \u2022\u00A0")
    }

    Text(
        text = fullSummary,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = textColor,
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FeaturedArtistsSection(
    featuredArtists: List<Artist>,
    playbackState: PlaybackUiState,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredArtists.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val title = if (featuredArtists.size == 1) "Featured Artist" else "Featured Artists"
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(featuredArtists, key = { it.id }) { artist ->
                CollaboratorArtistCard(
                    artist = artist,
                    playingArtist = playbackState.artist,
                    playingAlbum = playbackState.album,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onArtistClick(artist.id) }
                )
            }
        }
    }
}

private fun LazyListScope.albumSongsListItems(
    songsByDisc: Map<Int, List<Song>>,
    hasMultipleDiscs: Boolean,
    songs: List<Song>,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    onPlaySong: (Song) -> Unit,
    toggleSongSelection: (Song) -> Unit,
    onMoreClick: (Offset, IntSize, Song) -> Unit
) {
    if (hasMultipleDiscs) {
        songsByDisc.forEach { (discNumber, discSongs) ->
            item(key = "disc_header_$discNumber") {
                Text(
                    text = "Disc $discNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 8.dp,
                            bottom = 4.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                )
            }
            itemsIndexed(discSongs, key = { _, song -> song.id }) { _, song ->
                val isSelected = selectedSongIds.contains(song.id)
                SongListItem(
                    song = song,
                    onClick = {
                        if (isSelectionMode) {
                            toggleSongSelection(song)
                        } else {
                            onPlaySong(song)
                        }
                    },
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    showTrackNumber = true,
                    onLongClick = { toggleSongSelection(song) },
                    onMoreClick = { offset, size ->
                        onMoreClick(offset, size, song)
                    }
                )
            }
        }
    } else {
        itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
            val isSelected = selectedSongIds.contains(song.id)
            SongListItem(
                song = song,
                onClick = {
                    if (isSelectionMode) {
                        toggleSongSelection(song)
                    } else {
                        onPlaySong(song)
                    }
                },
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                showTrackNumber = true,
                onLongClick = { toggleSongSelection(song) },
                onMoreClick = { offset, size ->
                    onMoreClick(offset, size, song)
                }
            )
        }
    }
}

@Composable
fun SongCardItem(
    song: Song,
    trackIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = Color.Transparent,
    isCurrentSong: Boolean = false,
    onAddToQueue: ((Offset, IntSize) -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        isCurrentSong -> MaterialTheme.colorScheme.primaryContainer
        else -> cardColor
    }

    val titleColor = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        isCurrentSong -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val artistColor = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        isCurrentSong -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val trackNumberColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isCurrentSong -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    val itemShape = when {
        isSelected || isCurrentSong -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(16.dp)
    }

    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val view = LocalView.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = itemShape
                    )
                } else Modifier
            )
            .clip(itemShape)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            ),
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
            // Track number container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .onGloballyPositioned { coords ->
                        itemCoordinates = coords
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = (song.trackNumber ?: trackIndex).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = trackNumberColor,
                        textAlign = TextAlign.Center
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

            if (isSelectionMode) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (onAddToQueue != null) {
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

private fun formatAlbumTotalTime(totalDurationMs: Long): String {
    if (totalDurationMs <= 0) return ""
    val totalMinutes = ceil(totalDurationMs / 60000.0).toLong()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "$hours hr $minutes min"
        hours > 0 -> "$hours hr"
        minutes == 1L -> "1 min"
        else -> "$minutes mins"
    }
}

// -----------------------------------------------------------------------------
// PREVIEWS
// -----------------------------------------------------------------------------

private val samplePreviewAlbum = Album(
    id = "album_1",
    name = "Lorem Ipsum Dolor Sit Amet",
    artist = "Lorem Ipsum",
    year = 2024,
    artworkUri = null,
    trackCount = 12
)

private val samplePreviewPrimaryArtist = Artist(
    id = "lorem_ipsum",
    name = "Lorem Ipsum",
    trackCount = 12
)

private val samplePreviewFeaturedArtists = listOf(
    Artist(id = "dolor_sit", name = "Dolor Sit", trackCount = 2),
    Artist(id = "amet_consectetur", name = "Amet Consectetur", trackCount = 1),
    Artist(id = "adipiscing_elit", name = "Adipiscing Elit", trackCount = 1),
    Artist(id = "sed_do", name = "Sed Do", trackCount = 1)
)

private val samplePreviewSongsMultiDisc = listOf(
    Song(
        id = "d1_1",
        uri = Uri.EMPTY,
        title = "Lorem Ipsum Dolor",
        artist = "Lorem Ipsum",
        album = "Lorem Ipsum Dolor Sit Amet",
        durationMs = 274000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2024,
        artworkUri = null,
        fileName = "d1_track1.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum"
    ),
    Song(
        id = "d1_2",
        uri = Uri.EMPTY,
        title = "Sit Amet Consectetur",
        artist = "Lorem Ipsum feat. Dolor Sit",
        album = "Lorem Ipsum Dolor Sit Amet",
        durationMs = 248000L,
        trackNumber = 2,
        discNumber = 1,
        year = 2024,
        artworkUri = null,
        fileName = "d1_track2.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum, Dolor Sit"
    ),
    Song(
        id = "d2_1",
        uri = Uri.EMPTY,
        title = "Adipiscing Elit Sed",
        artist = "Lorem Ipsum",
        album = "Lorem Ipsum Dolor Sit Amet",
        durationMs = 264000L,
        trackNumber = 1,
        discNumber = 2,
        year = 2024,
        artworkUri = null,
        fileName = "d2_track1.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum"
    ),
    Song(
        id = "d2_2",
        uri = Uri.EMPTY,
        title = "Eiusmod Tempor Incididunt",
        artist = "Lorem Ipsum",
        album = "Lorem Ipsum Dolor Sit Amet",
        durationMs = 382000L,
        trackNumber = 2,
        discNumber = 2,
        year = 2024,
        artworkUri = null,
        fileName = "d2_track2.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum"
    )
)

private val samplePreviewPlaybackState = PlaybackUiState(
    trackTitle = "Sit Amet Consectetur",
    artist = "Lorem Ipsum feat. Dolor Sit",
    album = "Lorem Ipsum Dolor Sit Amet",
    isPlaying = true
)

private val samplePreviewPlaylists = listOf(
    Playlist(id = 1, name = "Lorem Playlist", songCount = 12),
    Playlist(id = 2, name = "Ipsum Playlist", songCount = 45)
)

@Preview(
    name = "Portrait Light",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "Album Detail Screen",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun AlbumDetailScreenPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumDetailContent(
                album = samplePreviewAlbum,
                primaryArtist = samplePreviewPrimaryArtist,
                songs = samplePreviewSongsMultiDisc,
                featuredArtists = samplePreviewFeaturedArtists,
                playbackState = samplePreviewPlaybackState,
                playlists = samplePreviewPlaylists,
                onBack = {},
                onArtistClick = {}
            )
        }
    }
}
