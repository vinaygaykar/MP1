package io.github.vinaygaykar.mp1.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.vinaygaykar.mp1.ui.playlist.PlaylistDetailActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import android.app.Activity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import io.github.vinaygaykar.mp1.domain.models.Playlist
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import io.github.vinaygaykar.mp1.R
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.components.ExpressiveScrollBar
import io.github.vinaygaykar.mp1.ui.components.extractFastScrollGlyph
import io.github.vinaygaykar.mp1.ui.settings.SettingsViewModel
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.ui.theme.isArtistMatchingPlayingTrack
import io.github.vinaygaykar.mp1.ui.theme.rememberMorphingArtistShape
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

data class FlyingArtworkItem(
    val id: Long = System.currentTimeMillis() + (0..1000000).random(),
    val artworkUri: Uri?,
    val startOffset: Offset,
    val startSize: IntSize
)

@Composable
fun LibraryScreen(
    bottomPadding: Dp = 0.dp,
    isPlayerActive: Boolean = false,
    miniPlayerTargetOffset: Offset? = null,
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val useCleanAlbumGrid by viewModel.useCleanAlbumGrid.collectAsState()

    val isScanning by remember(playerViewModel) {
        playerViewModel.uiState.map { it.isScanInProgress }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val currentFolder by settingsViewModel.currentScanningFolder.collectAsState()

    LibraryContent(
        playlists = playlists,
        songs = songs,
        albums = albums,
        artists = artists,
        genres = genres,
        useCleanAlbumGrid = useCleanAlbumGrid,
        bottomPadding = bottomPadding,
        isPlayerActive = isPlayerActive,
        miniPlayerTargetOffset = miniPlayerTargetOffset,
        isScanning = isScanning,
        currentFolder = currentFolder,
        onBulgeMiniPlayer = onBulgeMiniPlayer,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onOpenScanLocations = onOpenScanLocations,
        onPlaySong = { song, songList -> viewModel.playSong(song, songList) },
        onAddToQueue = { viewModel.addToQueue(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onImportPlaylist = { viewModel.importPlaylist(it) },
        onAddSongsToPlaylist = { playlistId, songsToAdd ->
            viewModel.addSongsToPlaylist(playlistId, songsToAdd)
        },
        onCreatePlaylistWithSongs = { name, songsToAdd ->
            viewModel.createPlaylistAndAddSongs(name, songsToAdd)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    playlists: List<Playlist> = emptyList(),
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    genres: List<Genre> = emptyList(),
    useCleanAlbumGrid: Boolean = false,
    bottomPadding: Dp = 0.dp,
    initialScrolledDown: Boolean = false,
    isPlayerActive: Boolean = false,
    miniPlayerTargetOffset: Offset? = null,
    isScanning: Boolean = false,
    currentFolder: String? = null,
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onAddToQueue: (Song) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onImportPlaylist: (Uri) -> Unit = {},
    onAddSongsToPlaylist: (Long, List<Song>) -> Unit = { _, _ -> },
    onCreatePlaylistWithSongs: (String, List<Song>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current

    val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPlaylistSelectionSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistForSelectionDialog by remember { mutableStateOf(false) }

    val importPlaylistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportPlaylist(it) }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> onCreatePlaylist(name) }
        )
    }

    val tabs = listOf("Playlist", "Songs", "Albums", "Artists"/*, "Genres"*/)
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })

    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
    var showMultiSelectionSheet by remember { mutableStateOf(false) }

    val selectedSongs = remember(songs, selectedSongIds) {
        songs.filter { selectedSongIds.contains(it.id) }
    }

    if (showCreatePlaylistForSelectionDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistForSelectionDialog = false },
            onCreate = { name ->
                onCreatePlaylistWithSongs(name, selectedSongs)
                selectedSongIds = emptySet()
            }
        )
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

    LaunchedEffect(pagerState.currentPage) {
        selectedSongIds = emptySet()
    }

    // Measure top header height dynamically
    var collapsibleHeaderHeightPx by remember { mutableFloatStateOf(0f) }
    var selectionRowHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isSelectionMode, pagerState.currentPage) {
        if (!(isSelectionMode && pagerState.currentPage == 1)) {
            selectionRowHeightPx = 0f
        }
    }

    val topBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = if (initialScrolledDown) -500f else -1000f,
        initialHeightOffset = if (initialScrolledDown) -500f else 0f,
        initialContentOffset = 0f
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topBarState)

    LaunchedEffect(collapsibleHeaderHeightPx) {
        if (collapsibleHeaderHeightPx > 0f) {
            scrollBehavior.state.heightOffsetLimit = -collapsibleHeaderHeightPx
            if (initialScrolledDown) {
                scrollBehavior.state.heightOffset = -collapsibleHeaderHeightPx
            }
        }
    }

    val isFloatingVisible by remember {
        derivedStateOf {
            scrollBehavior.state.collapsedFraction > 0.9f && !(isSelectionMode && pagerState.currentPage == 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .onGloballyPositioned { coords ->
                rootOffsetInWindow = coords.positionInRoot()
                rootSize = coords.size
            }
    ) {
        val currentMiniPlayerTargetOffset = miniPlayerTargetOffset
        val targetX = if (currentMiniPlayerTargetOffset != null) {
            currentMiniPlayerTargetOffset.x - rootOffsetInWindow.x
        } else {
            rootSize.width.toFloat() - with(density) { (8.dp + 20.dp + 56.dp).toPx() }
        }

        val targetY = if (currentMiniPlayerTargetOffset != null) {
            currentMiniPlayerTargetOffset.y - rootOffsetInWindow.y
        } else if (rootSize.height > 0) {
            rootSize.height.toFloat() - with(density) { (bottomPadding + 64.dp).toPx() }
        } else {
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.heightPixels.toFloat() - with(density) { (bottomPadding + 64.dp).toPx() }
        }

        val targetLocalOffset = Offset(targetX, targetY)

        flyingArtworks.forEach { item ->
            key(item.id) {
                FlyingArtworkOverlay(
                    item = item,
                    rootOffset = rootOffsetInWindow,
                    targetOffsetInLocal = targetLocalOffset,
                    onAnimationComplete = {
                        onBulgeMiniPlayer()
                        flyingArtworks.remove(item)
                    }
                )
            }
        }
        // Layer 1: Collapsible Top Bar Header (Title + Tabs)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = scrollBehavior.state.heightOffset
                    alpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)
                }
                .background(MaterialTheme.colorScheme.surface)
                .onGloballyPositioned { coordinates ->
                    collapsibleHeaderHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            // Title Row with App Name + Search & Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            CompositionLocalProvider(
                LocalRippleConfiguration provides null
            ) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            val currentPage = pagerState.currentPage.coerceIn(0, tabPositions.size - 1)
                            val currentTab = tabPositions[currentPage]
                            val fraction = pagerState.currentPageOffsetFraction

                            val targetPage = if (fraction > 0) {
                                (currentPage + 1).coerceAtMost(tabPositions.size - 1)
                            } else if (fraction < 0) {
                                (currentPage - 1).coerceAtLeast(0)
                            } else {
                                currentPage
                            }
                            val targetTab = tabPositions[targetPage]

                            val absFraction = fraction.absoluteValue
                            val currentLeft = currentTab.left
                            val currentRight = currentTab.right
                            val targetLeft = targetTab.left
                            val targetRight = targetTab.right

                            val indicatorLeft = currentLeft + (targetLeft - currentLeft) * absFraction
                            val indicatorRight =
                                currentRight + (targetRight - currentRight) * absFraction

                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = indicatorLeft)
                                    .width(indicatorRight - indicatorLeft)
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .zIndex(-1f)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        val interactionSource = remember { MutableInteractionSource() }

                        val animatedLetterSpacing by animateFloatAsState(
                            targetValue = if (isSelected) 0.5f else -0.5f,
                            animationSpec = tween(durationMillis = 250),
                            label = "letterSpacing"
                        )
                        val animatedColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(durationMillis = 250),
                            label = "color"
                        )

                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            interactionSource = interactionSource,
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = animatedLetterSpacing.sp
                                    ),
                                    color = animatedColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }
        }

        // Layer 1.5: Sticky Selection Action Row (positioned below tabs when uncollapsed, sticky at top when collapsed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = (collapsibleHeaderHeightPx + scrollBehavior.state.heightOffset).coerceAtLeast(0f)
                }
                .zIndex(1f)
        ) {
            AnimatedVisibility(
                visible = isSelectionMode && pagerState.currentPage == 1,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { coordinates ->
                        if (isSelectionMode && pagerState.currentPage == 1) {
                            selectionRowHeightPx = coordinates.size.height.toFloat()
                        }
                    }
            ) {
                SelectionActionRow(
                    selectedCount = selectedSongIds.size,
                    onSelectAll = {
                        selectedSongIds = songs.map { it.id }.toSet()
                    },
                    onDeselect = {
                        selectedSongIds = emptySet()
                    },
                    onOptionsClick = {
                        showMultiSelectionSheet = true
                    }
                )
            }
        }

        // Layer 0: Content Surface (occupies full screen height, translates UP to sit below sticky headers)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY =
                        (collapsibleHeaderHeightPx + scrollBehavior.state.heightOffset).coerceAtLeast(0f) + selectionRowHeightPx
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PlaylistGrid(
                        playlists = playlists,
                        onCreateClick = { showCreateDialog = true },
                        onImportClick = { importPlaylistLauncher.launch("*/*") },
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                    )

                    1 -> SongList(
                        songs = songs,
                        onSongClick = { song ->
                            if (isSelectionMode) {
                                toggleSongSelection(song)
                            } else {
                                onPlaySong(song, songs)
                            }
                        },
                        onSongLongClick = { song ->
                            toggleSongSelection(song)
                        },
                        selectedSongIds = selectedSongIds,
                        isSelectionMode = isSelectionMode,
                        onSwipeRight = { song, offset, size ->
                            onAddToQueue(song)
                            if (isPlayerActive) {
                                flyingArtworks.add(
                                    FlyingArtworkItem(
                                        artworkUri = song.artworkUri,
                                        startOffset = offset,
                                        startSize = size
                                    )
                                )
                            }
                        },
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                    )

                    2 -> {
                        if (useCleanAlbumGrid) {
                            AlbumGridClean(
                                albums = albums,
                                onAlbumClick = { album, offset, size ->
                                    val intent =
                                        Intent(context, AlbumDetailActivity::class.java).apply {
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
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            )
                        } else {
                            AlbumGridPunk(
                                albums = albums,
                                onAlbumClick = { album ->
                                    val intent =
                                        Intent(context, AlbumDetailActivity::class.java).apply {
                                            putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id)
                                        }
                                    launchActivityWithScaleUp(context = context, intent = intent, view = view)
                                },
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            )
                        }
                    }

                    3 -> ArtistGrid(
                        artists = artists,
                        onArtistClick = { artist, offset, size ->
                            val intent =
                                Intent(context, ArtistDetailActivity::class.java).apply {
                                    putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artist.id)
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                    )
//
//                    4 -> GenreList(
//                        genres = genres,
//                        onGenreClick = { genre ->
//                            val intent =
//                                Intent(context, GenreDetailActivity::class.java).apply {
//                                    putExtra(GenreDetailActivity.EXTRA_GENRE_ID, genre.id)
//                                    putExtra(GenreDetailActivity.EXTRA_GENRE_NAME, genre.name)
//                                }
//                            launchActivityWithScaleUp(context = context, intent = intent, view = view)
//                        },
//                        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
//                    )
                }
            }
        }

        // Layer 2: Floating Category Dropdown & Action Buttons
        if (isFloatingVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Dropdown
                Box {
                    Surface(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = tabs[pagerState.currentPage],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select category",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                leadingIcon = {
                                    if (pagerState.currentPage == index) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Action Button: Search
                Surface(
                    onClick = onSearchClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Multi-selection Bottom Sheet
        if (showMultiSelectionSheet) {
            MultiSelectionBottomSheet(
                selectedSongs = selectedSongs,
                onDismiss = { showMultiSelectionSheet = false },
                onPlayAll = {
                    if (selectedSongs.isNotEmpty()) {
                        onPlaySong(selectedSongs.first(), selectedSongs)
                        selectedSongIds = emptySet()
                    }
                },
                onAddToPlaylist = {
                    showMultiSelectionSheet = false
                    showPlaylistSelectionSheet = true
                },
                onAddToQueue = {
                    selectedSongs.forEach { onAddToQueue(it) }
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
                    onAddSongsToPlaylist(playlist.id, selectedSongs)
                    selectedSongIds = emptySet()
                },
                onCreateNewPlaylist = {
                    showPlaylistSelectionSheet = false
                    showCreatePlaylistForSelectionDialog = true
                }
            )
        }

        // Scan in Progress Banner (Bottom)
        val displayFolder = remember(currentFolder) {
            currentFolder
                ?.removePrefix("primary:")
                ?.removePrefix("primary")
                ?.trim()
        }

        AnimatedVisibility(
            visible = isScanning,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 8.dp)
                .padding(horizontal = 16.dp)
                .zIndex(10f)
        ) {
            Surface(
                onClick = onOpenScanLocations,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Library scan in progress...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!displayFolder.isNullOrBlank()) {
                            Text(
                                text = "Scanning $displayFolder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Scan Directories",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private val previewSongs = listOf(
    Song(
        id = "1",
        uri = Uri.EMPTY,
        title = "Sample Song 1",
        artist = "Sample Artist 1",
        album = "Sample Album 1",
        durationMs = 210000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2024,
        artworkUri = null,
        fileName = "song1.mp3",
        lastModified = System.currentTimeMillis()
    ),
    Song(
        id = "2",
        uri = Uri.EMPTY,
        title = "Sample Song 2",
        artist = "Sample Artist 2",
        album = "Sample Album 2",
        durationMs = 180000L,
        trackNumber = 2,
        discNumber = 1,
        year = 2024,
        artworkUri = null,
        fileName = "song2.mp3",
        lastModified = System.currentTimeMillis()
    )
)

private val previewAlbums = listOf(
    Album(
        id = "1",
        name = "Sample Album 1",
        artist = "Sample Artist 1",
        year = 2024,
        artworkUri = null,
        trackCount = 10
    )
)

private val previewArtists = listOf(
    Artist(
        id = "1",
        name = "Sample Artist 1",
        trackCount = 10
    )
)

private val previewGenres = listOf(
    Genre(
        id = "1",
        name = "Rock",
        trackCount = 12
    ),
    Genre(
        id = "2",
        name = "Pop",
        trackCount = 8
    )
)

@Preview(name = "Expanded State", showBackground = true)
@Composable
fun LibraryScreenExpandedPreview() {
    MusicPlayerTheme {
        LibraryContent(
            songs = previewSongs,
            albums = previewAlbums,
            artists = previewArtists,
            genres = previewGenres,
            initialScrolledDown = false
        )
    }
}

@Preview(name = "Collapsed State", showBackground = true)
@Composable
fun LibraryScreenCollapsedPreview() {
    MusicPlayerTheme {
        LibraryContent(
            songs = previewSongs,
            albums = previewAlbums,
            artists = previewArtists,
            genres = previewGenres,
            initialScrolledDown = true
        )
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSwipeRight: (Song, Offset, IntSize) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    selectedSongIds: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onSongLongClick: (Song) -> Unit = {}
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No songs found. Try scanning a folder in Settings.")
        }
    } else {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                items(songs, key = { it.id }) { song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    val onClickLambda = remember(song.id, isSelectionMode) {
                        { onSongClick(song) }
                    }
                    val onSwipeRightLambda = remember(song.id) {
                        { offset: Offset, size: IntSize -> onSwipeRight(song, offset, size) }
                    }
                    val onLongClickLambda = remember(song.id) {
                        { onSongLongClick(song) }
                    }
                    SongListItem(
                        song = song,
                        onClick = onClickLambda,
                        onSwipeRight = onSwipeRightLambda,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = onLongClickLambda
                    )
                }
            }
            ExpressiveScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                listState = listState,
                dragLabelProvider = { index ->
                    if (index in songs.indices) {
                        extractFastScrollGlyph(songs[index].title)
                    } else null
                }
            )
        }
    }
}

@Composable
fun FlyingArtworkOverlay(
    item: FlyingArtworkItem,
    rootOffset: Offset,
    targetOffsetInLocal: Offset,
    onAnimationComplete: () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(item.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 420,
                easing = FastOutSlowInEasing
            )
        )
        onAnimationComplete()
    }

    val p = animProgress.value

    val startX = item.startOffset.x - rootOffset.x
    val startY = item.startOffset.y - rootOffset.y
    val targetX = targetOffsetInLocal.x
    val targetY = targetOffsetInLocal.y

    val currentX = lerp(startX, targetX, p)
    val linearY = lerp(startY, targetY, p)
    val arcOffset = -30f * (1f - p) * p
    val currentY = linearY + arcOffset

    val sizeDp = 56.dp
    val currentAlpha = if (p > 0.5f) ((1f - p) / 0.5f).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(sizeDp)
            .zIndex(999f)
            .graphicsLayer {
                alpha = currentAlpha
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
        AsyncImage(
            model = item.artworkUri,
            contentDescription = null,
            error = musicPlaceholder,
            placeholder = musicPlaceholder,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun ArtistGrid(
    artists: List<Artist>,
    onArtistClick: (Artist, Offset, IntSize) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playingArtist by remember(playerViewModel) { playerViewModel.uiState.map { it.artist }.distinctUntilChanged() }.collectAsState(initial = "")
    val playingAlbum by remember(playerViewModel) { playerViewModel.uiState.map { it.album }.distinctUntilChanged() }.collectAsState(initial = "")
    val isPlaying by remember(playerViewModel) { playerViewModel.uiState.map { it.isPlaying }.distinctUntilChanged() }.collectAsState(initial = false)

    if (artists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No artists found.")
        }
    } else {
        val gridState = rememberLazyGridState()
        val mergedPadding = PaddingValues(
            start = 8.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = mergedPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artists) { artist ->
                    ArtistGridItem(
                        artist = artist,
                        playingArtist = playingArtist,
                        playingAlbum = playingAlbum,
                        isPlaying = isPlaying,
                        onClick = { offset, size -> onArtistClick(artist, offset, size) },
                        onFetchImageUrl = { name, id -> libraryViewModel.getArtistImageUrl(name, id) }
                    )
                }
            }
            ExpressiveScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                gridState = gridState,
                dragLabelProvider = { index ->
                    if (index in artists.indices) {
                        extractFastScrollGlyph(artists[index].name)
                    } else null
                }
            )
        }
    }
}

@Composable
fun ArtistGridItem(
    artist: Artist,
    onClick: (Offset, IntSize) -> Unit,
    playingArtist: String = "",
    playingAlbum: String = "",
    isPlaying: Boolean = false,
    onFetchImageUrl: suspend (String, String) -> String? = { _, _ -> null }
) {
    var displayImageUrl by remember(artist.imageUrl) { mutableStateOf(artist.imageUrl) }

    LaunchedEffect(artist.id, artist.name, artist.imageUrl) {
        if (displayImageUrl.isNullOrEmpty()) {
            val fetchedUrl = onFetchImageUrl(artist.name, artist.id)
            if (!fetchedUrl.isNullOrEmpty()) {
                displayImageUrl = fetchedUrl
            }
        }
    }

    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isMatchingArtist = remember(artist.name, playingArtist, playingAlbum) {
        isArtistMatchingPlayingTrack(
            artistName = artist.name,
            playingArtistString = playingArtist,
            playingAlbumString = playingAlbum
        )
    }

    val isMorphing = isPlaying && isMatchingArtist

    val artistShape = rememberMorphingArtistShape(
        artistId = artist.id,
        artistName = artist.name,
        isMorphing = isMorphing
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> itemCoordinates = coords }
            .clickable {
                val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                val size = itemCoordinates?.size ?: IntSize.Zero
                onClick(offset, size)
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Artist Image with custom polygon shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artistShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!displayImageUrl.isNullOrEmpty()) {
                val personPlaceholder = rememberVectorPainter(Icons.Default.Person)
                AsyncImage(
                    model = displayImageUrl,
                    contentDescription = artist.name,
                    error = personPlaceholder,
                    placeholder = personPlaceholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GenreList(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (genres.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No genres found.")
        }
    } else {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(genres, key = { it.id }) { genre ->
                    GenreListItem(
                        genre = genre,
                        onClick = { onGenreClick(genre) }
                    )
                }
            }
            ExpressiveScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                listState = listState,
                dragLabelProvider = { index ->
                    if (index in genres.indices) {
                        extractFastScrollGlyph(genres[index].name)
                    } else null
                }
            )
        }
    }
}

@Composable
fun GenreListItem(
    genre: Genre,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = genre.name.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
    }
}

@Composable
fun PlaylistGrid(
    playlists: List<Playlist>,
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Create button
                    Surface(
                        onClick = onCreateClick,
                        shape = RoundedCornerShape(
                            topStart = 22.dp,
                            bottomStart = 22.dp,
                            topEnd = 6.dp,
                            bottomEnd = 6.dp
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Import button
                    Surface(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(
                            topStart = 6.dp,
                            bottomStart = 6.dp,
                            topEnd = 22.dp,
                            bottomEnd = 22.dp
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                PlaylistItemCard(
                    playlist = playlist,
                    onClick = {
                        val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                            putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id)
                            putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.name)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: Playlist,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Playlist",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onCreate(playlistName.trim())
                        onDismiss()
                    }
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


