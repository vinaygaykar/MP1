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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.library.tabs.AlbumsTab
import io.github.vinaygaykar.mp1.ui.library.tabs.ArtistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistMultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet
import io.github.vinaygaykar.mp1.ui.library.tabs.SongList
import io.github.vinaygaykar.mp1.ui.settings.SettingsViewModel
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
        playerViewModel.playbackState.map { it.isScanInProgress }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val currentFolder by settingsViewModel.currentScanningFolder.collectAsState()
    val importError by viewModel.importError.collectAsState()

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
        importError = importError,
        onDismissImportError = { viewModel.clearImportError() },
        onBulgeMiniPlayer = onBulgeMiniPlayer,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onOpenScanLocations = onOpenScanLocations,
        onPlaySong = { song, songList -> viewModel.playSong(song, songList) },
        onAddToQueue = { viewModel.addToQueue(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDeletePlaylists = { viewModel.deletePlaylists(it) },
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
    importError: String? = null,
    onDismissImportError: () -> Unit = {},
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onAddToQueue: (Song) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylists: (Set<Long>) -> Unit = {},
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
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportPlaylist(it) }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> onCreatePlaylist(name) }
        )
    }

    if (importError != null) {
        AlertDialog(
            onDismissRequest = onDismissImportError,
            title = {
                Text(
                    text = "Import Failed",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = importError,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissImportError) {
                    Text("OK")
                }
            }
        )
    }

    val tabs = listOf("Playlist", "Songs", "Albums", "Artists"/*, "Genres"*/)
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })

    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedSongIds.isNotEmpty()
    val isPlaylistSelectionMode = selectedPlaylistIds.isNotEmpty()
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showPlaylistMultiSelectionSheet by remember { mutableStateOf(false) }

    val selectedSongs = remember(songs, selectedSongIds) {
        songs.filter { selectedSongIds.contains(it.id) }
    }
    val selectedPlaylists = remember(playlists, selectedPlaylistIds) {
        playlists.filter { selectedPlaylistIds.contains(it.id) }
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

    fun togglePlaylistSelection(playlist: Playlist) {
        selectedPlaylistIds = if (selectedPlaylistIds.contains(playlist.id)) {
            selectedPlaylistIds - playlist.id
        } else {
            selectedPlaylistIds + playlist.id
        }
    }

    BackHandler(enabled = isSelectionMode || isPlaylistSelectionMode) {
        selectedSongIds = emptySet()
        selectedPlaylistIds = emptySet()
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedSongIds = emptySet()
        selectedPlaylistIds = emptySet()
    }

    // Measure top header height dynamically
    var collapsibleHeaderHeightPx by remember { mutableFloatStateOf(0f) }
    var noticeBannerHeightPx by remember { mutableFloatStateOf(0f) }
    var selectionRowHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isScanning) {
        if (!isScanning) {
            noticeBannerHeightPx = 0f
        }
    }

    LaunchedEffect(isSelectionMode, isPlaylistSelectionMode, pagerState.currentPage) {
        if (!(isSelectionMode && pagerState.currentPage == 1) && !(isPlaylistSelectionMode && pagerState.currentPage == 0)) {
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
            scrollBehavior.state.collapsedFraction > 0.9f &&
                !(isSelectionMode && pagerState.currentPage == 1) &&
                !(isPlaylistSelectionMode && pagerState.currentPage == 0)
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

        // Main 1-Column Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Layer 1: Collapsible Top Bar Header (Title + Tabs)
            var maxMeasuredHeaderHeightPx by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (maxMeasuredHeaderHeightPx > 0f) {
                            val targetPx = (maxMeasuredHeaderHeightPx + scrollBehavior.state.heightOffset).coerceAtLeast(0f)
                            Modifier.height(with(density) { targetPx.toDp() })
                        } else Modifier
                    )
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surface)
                    .graphicsLayer {
                        alpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)
                    }
                    .onGloballyPositioned { coordinates ->
                        val h = coordinates.size.height.toFloat()
                        if (h > maxMeasuredHeaderHeightPx) {
                            maxMeasuredHeaderHeightPx = h
                            collapsibleHeaderHeightPx = h
                        }
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

            // Layer 1.2: Scan in Progress Banner
            val displayFolder = remember(currentFolder) {
                currentFolder
                    ?.removePrefix("primary:")
                    ?.removePrefix("primary")
                    ?.trim()
            }

            AnimatedVisibility(
                visible = isScanning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        noticeBannerHeightPx = if (isScanning) coordinates.size.height.toFloat() else 0f
                    }
            ) {
                NoticeBanner(
                    title = "Library scan in progress...",
                    subtitle = if (!displayFolder.isNullOrBlank()) "Scanning $displayFolder" else null,
                    onClick = onOpenScanLocations,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Scan Directories",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            // Layer 1.5: Sticky Selection Action Row
            AnimatedVisibility(
                visible = (isSelectionMode && pagerState.currentPage == 1) || (isPlaylistSelectionMode && pagerState.currentPage == 0),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { coordinates ->
                        if ((isSelectionMode && pagerState.currentPage == 1) || (isPlaylistSelectionMode && pagerState.currentPage == 0)) {
                            selectionRowHeightPx = coordinates.size.height.toFloat()
                        }
                    }
            ) {
                if (pagerState.currentPage == 0) {
                    SelectionActionRow(
                        selectedCount = selectedPlaylistIds.size,
                        onSelectAll = {
                            selectedPlaylistIds = playlists.map { it.id }.toSet()
                        },
                        onDeselect = {
                            selectedPlaylistIds = emptySet()
                        },
                        onOptionsClick = {
                            showPlaylistMultiSelectionSheet = true
                        }
                    )
                } else if (pagerState.currentPage == 1) {
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

            // Layer 0: Content Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            onImportClick = {
                                importPlaylistLauncher.launch(
                                    arrayOf(
                                        "audio/x-mpegurl",
                                        "audio/mpegurl",
                                        "application/x-mpegurl",
                                        "application/vnd.apple.mpegurl",
                                        "audio/x-scpls",
                                        "text/plain",
                                        "text/x-m3u"
                                    )
                                )
                            },
                            selectedPlaylistIds = selectedPlaylistIds,
                            isSelectionMode = isPlaylistSelectionMode,
                            onPlaylistClick = { playlist ->
                                if (isPlaylistSelectionMode) {
                                    togglePlaylistSelection(playlist)
                                } else {
                                    val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                                        putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.id)
                                        putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.name)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onPlaylistLongClick = { playlist ->
                                togglePlaylistSelection(playlist)
                            },
                            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                        )

                        1 -> {
                            val effectiveHeaderPx = collapsibleHeaderHeightPx + (if (isScanning) noticeBannerHeightPx else 0f)
                            val headerHeightDp = remember(effectiveHeaderPx, density) {
                                if (effectiveHeaderPx > 0f) {
                                    with(density) { effectiveHeaderPx.toDp() }
                                } else {
                                    104.dp
                                }
                            }
                            SongList(
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
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                                headerHeight = headerHeightDp
                            )
                        }

                        2 -> AlbumsTab(
                            albums = albums,
                            useCleanAlbumGrid = useCleanAlbumGrid,
                            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                        )

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
                    }
                }
            }
        }

        // Layer 2: Floating Category Dropdown & Action Buttons
        if (isFloatingVisible) {
            val noticeBannerHeightDp = with(density) {
                if (isScanning) noticeBannerHeightPx.toDp() else 0.dp
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = noticeBannerHeightDp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(3f),
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
            PlaylistPickerBottomSheet(
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

        // Playlist Multi-selection Bottom Sheet
        if (showPlaylistMultiSelectionSheet) {
            PlaylistMultiSelectionBottomSheet(
                selectedPlaylists = selectedPlaylists,
                onDismiss = { showPlaylistMultiSelectionSheet = false },
                onDeletePlaylists = {
                    onDeletePlaylists(selectedPlaylistIds)
                    selectedPlaylistIds = emptySet()
                }
            )
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

@Preview(name = "Scanning Banner Active", showBackground = true)
@Composable
fun LibraryScreenWithBannerPreview() {
    MusicPlayerTheme {
        LibraryContent(
            songs = previewSongs,
            albums = previewAlbums,
            artists = previewArtists,
            genres = previewGenres,
            isScanning = true,
            currentFolder = "/storage/emulated/0/Music",
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
