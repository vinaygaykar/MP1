package io.github.vinaygaykar.mp1.ui.library.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.components.SongHeroOverlayContainer
import io.github.vinaygaykar.mp1.ui.components.SongOverlayData
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.library.SongsMultiSelectOverlay
import io.github.vinaygaykar.mp1.ui.library.tabs.AlbumsTab
import io.github.vinaygaykar.mp1.ui.library.tabs.ArtistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.SongList

/**
 * Level 2 Screen: Category Detail List.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryDetailScreen(
    uiState: LibraryUiState,
    libraryState: LibraryState,
    displayFolder: String?,
    bottomPadding: Dp,
    detailProgress: Float,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenScanLocations: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (List<Song>) -> Unit,
    onAddToQueue: (List<Song>) -> Unit,
    onImportPlaylistLauncher: () -> Unit,
    onHideNowPlayingView: () -> Unit = {},
    onShowNowPlayingView: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (Long, String) -> Unit = { _, _ -> },
    onCheckHasOfflineLyrics: suspend (Song) -> Boolean = { false },
    isSplashActive: Boolean = false,
    isNowPlayingExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var noticeBannerHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.isScanning) {
        if (!uiState.isScanning) {
            noticeBannerHeightPx = 0f
        }
    }

    val isFloatingVisible by remember(scrollBehavior, libraryState) {
        derivedStateOf {
            scrollBehavior.state.collapsedFraction > 0.9f &&
                    !libraryState.isSelectionMode &&
                    !libraryState.isPlaylistSelectionMode
        }
    }

    val songsMap = remember(uiState.songs) { uiState.songs.associateBy { it.id } }
    val selectedSongsInSelectionOrder = remember(songsMap, libraryState.selectedSongIds) {
        libraryState.selectedSongIds.mapNotNull { songsMap[it] }
    }

    var activeOverlayData by remember { mutableStateOf<SongOverlayData?>(null) }
    var isCategoryEntranceComplete by remember(libraryState.lastOpenedCategory) { mutableStateOf(false) }

    LaunchedEffect(libraryState.lastOpenedCategory, isSplashActive, detailProgress > 0.85f) {
        if (!isSplashActive && (detailProgress > 0.85f)) {
            delay(650.milliseconds)
            isCategoryEntranceComplete = true
        } else if (detailProgress < 0.05f) {
            isCategoryEntranceComplete = false
        }
    }

    val isSongsSelectionActive = libraryState.lastOpenedCategory == LibraryCategory.SONGS && libraryState.isSelectionMode
    val isActionPopupOpen = isSongsSelectionActive && libraryState.showMultiSelectionSheet

    // Back Handler Level 1: When expanded, back press collapses the overlay back to compact mode
    BackHandler(enabled = !isNowPlayingExpanded && isActionPopupOpen) {
        libraryState.showMultiSelectionSheet = false
    }

    // Back Handler Level 2: When collapsed in selection mode, back press exits selection mode and deselects all
    BackHandler(enabled = !isNowPlayingExpanded && isSongsSelectionActive && !libraryState.showMultiSelectionSheet) {
        libraryState.clearSongSelection()
    }

    LaunchedEffect(libraryState.isSelectionMode) {
        if (!libraryState.isSelectionMode) {
            libraryState.showMultiSelectionSheet = false
        }
    }

    LaunchedEffect(activeOverlayData != null, isActionPopupOpen) {
        if (activeOverlayData != null || isActionPopupOpen) {
            onHideNowPlayingView()
        } else {
            onShowNowPlayingView()
        }
    }

    val hazeState = remember { HazeState() }

    SongHeroOverlayContainer(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = (1f - detailProgress) * size.width
                clip = false
            },
        overlayData = activeOverlayData,
        onDismiss = { activeOverlayData = null },
        onPlayNext = { song -> onPlayNext(listOf(song)) },
        onAddToQueue = { song -> onAddToQueue(listOf(song)) },
        onAlbumClick = onAlbumClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .hazeSource(hazeState)
            ) {
                // Scan Notice Banner
                AnimatedVisibility(
                    visible = uiState.isScanning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            noticeBannerHeightPx =
                                if (uiState.isScanning) coordinates.size.height.toFloat() else 0f
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

                TopAppBar(
                    title = {
                        Text(
                            text = libraryState.lastOpenedCategory.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets,
                    scrollBehavior = scrollBehavior,
                )

                // Sticky Selection Row if Active (Preserved - replaced by floating OverlappingArtworksRow2 for SONGS)
                AnimatedVisibility(
                    visible = (libraryState.lastOpenedCategory == LibraryCategory.PLAYLISTS && libraryState.isPlaylistSelectionMode),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (libraryState.lastOpenedCategory == LibraryCategory.PLAYLISTS) {
                        SelectionActionRow(
                            selectedCount = libraryState.selectedPlaylistIds.size,
                            onSelectAll = { libraryState.selectAllPlaylists(uiState.playlists) },
                            onDeselect = { libraryState.clearPlaylistSelection() },
                            onOptionsClick = { libraryState.showPlaylistMultiSelectionSheet = true }
                        )
                    }
                }

                // Detail List Content Rendering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    when (libraryState.lastOpenedCategory) {
                        LibraryCategory.PLAYLISTS -> {
                            PlaylistGrid(
                                playlists = uiState.playlists,
                                onCreateClick = { libraryState.showCreateDialog = true },
                                onImportClick = onImportPlaylistLauncher,
                                selectedPlaylistIds = libraryState.selectedPlaylistIds,
                                isSelectionMode = libraryState.isPlaylistSelectionMode,
                                detailProgress = detailProgress,
                                categoryKey = libraryState.lastOpenedCategory,
                                isSplashActive = isSplashActive,
                                isCategoryEntranceComplete = isCategoryEntranceComplete,
                                onPlaylistClick = { playlist ->
                                    if (libraryState.isPlaylistSelectionMode) {
                                        libraryState.togglePlaylistSelection(playlist)
                                    } else {
                                        onPlaylistClick(playlist.id, playlist.name)
                                    }
                                },
                                onPlaylistLongClick = { playlist ->
                                    libraryState.togglePlaylistSelection(playlist)
                                },
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            )
                        }

                        LibraryCategory.SONGS -> {
                            SongList(
                                songs = uiState.songs,
                                detailProgress = detailProgress,
                                categoryKey = libraryState.lastOpenedCategory,
                                isSplashActive = isSplashActive,
                                isCategoryEntranceComplete = isCategoryEntranceComplete,
                                onSongClick = { song ->
                                    if (libraryState.isSelectionMode) {
                                        libraryState.toggleSongSelection(song)
                                    } else {
                                        onPlaySong(song, uiState.songs)
                                    }
                                },
                                onSongLongClick = { song ->
                                    libraryState.toggleSongSelection(song)
                                },
                                onSongMoreClick = { song, offset, size ->
                                    coroutineScope.launch {
                                        val hasLyrics = onCheckHasOfflineLyrics(song)
                                        activeOverlayData = SongOverlayData(song, offset, size, hasOfflineLyrics = hasLyrics)
                                    }
                                },
                                selectedSongIds = libraryState.selectedSongIds,
                                isSelectionMode = libraryState.isSelectionMode,
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                                headerHeight = 104.dp
                            )
                        }

                        LibraryCategory.ALBUMS -> {
                            AlbumsTab(
                                albums = uiState.albums,
                                onAlbumClick = { album -> onAlbumClick(album.id) },
                                detailProgress = detailProgress,
                                categoryKey = libraryState.lastOpenedCategory,
                                isSplashActive = isSplashActive,
                                isCategoryEntranceComplete = isCategoryEntranceComplete,
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            )
                        }

                        LibraryCategory.ARTISTS -> {
                            ArtistGrid(
                                artists = uiState.artists,
                                detailProgress = detailProgress,
                                categoryKey = libraryState.lastOpenedCategory,
                                isSplashActive = isSplashActive,
                                isCategoryEntranceComplete = isCategoryEntranceComplete,
                                onArtistClick = { artist -> onArtistClick(artist.id) },
                                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                            )
                        }

                        else -> {}
                    }
                }
            }

            // Floating Search Button visible when action bar is hidden (hidden when detail overlay is open)
            if (isFloatingVisible && activeOverlayData == null) {
                val noticeBannerHeightDp = with(density) {
                    if (uiState.isScanning) noticeBannerHeightPx.toDp() else 0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = noticeBannerHeightDp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(3f)
                ) {
                    Surface(
                        onClick = onSearchClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(44.dp)
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

            // Unified OverlappingArtworksRow2 floating overlay (Compact top bar when closed, full-screen scrollable overlay when expanded)
            val noticeBannerHeightDp = with(density) {
                if (uiState.isScanning) noticeBannerHeightPx.toDp() else 0.dp
            }

            val isActionPopupVisible = libraryState.showMultiSelectionSheet &&
                    libraryState.lastOpenedCategory == LibraryCategory.SONGS &&
                    libraryState.isSelectionMode

            SongsMultiSelectOverlay(
                songs = selectedSongsInSelectionOrder,
                visible = libraryState.lastOpenedCategory == LibraryCategory.SONGS && libraryState.isSelectionMode,
                isExpanded = isActionPopupVisible,
                onClick = {
                    libraryState.showMultiSelectionSheet = !libraryState.showMultiSelectionSheet
                },
                onDismiss = {
                    libraryState.showMultiSelectionSheet = false
                },
                onPlayNext = { activeSongs ->
                    if (activeSongs.isNotEmpty()) {
                        onPlayNext(activeSongs)
                        libraryState.clearSongSelection()
                    }
                },
                onAddToPlaylist = { activeSongs ->
                    libraryState.showMultiSelectionSheet = false
                    libraryState.showPlaylistSelectionSheet = true
                },
                onAddToQueue = { activeSongs ->
                    if (activeSongs.isNotEmpty()) {
                        onAddToQueue(activeSongs)
                        libraryState.clearSongSelection()
                    }
                },
                hazeState = hazeState,
                bottomPadding = bottomPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = noticeBannerHeightDp)
                    .align(Alignment.TopCenter)
                    .zIndex(5f)
            )

            // Left Side Shadow during Transition
            if (detailProgress > 0f && detailProgress < 1f) {
                val shadowAlpha = minOf(detailProgress / 0.05f, (1f - detailProgress) / 0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .offset(x = (-12).dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.03f * shadowAlpha),
                                    Color.Black.copy(alpha = 0.10f * shadowAlpha)
                                )
                            )
                        )
                )
            }
        }
    }
}
