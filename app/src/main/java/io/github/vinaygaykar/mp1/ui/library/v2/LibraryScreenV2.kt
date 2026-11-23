package io.github.vinaygaykar.mp1.ui.library.v2

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.library.DragValue
import io.github.vinaygaykar.mp1.ui.library.LibraryViewModel
import io.github.vinaygaykar.mp1.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun LibraryScreenV2(
    bottomPadding: Dp = 0.dp,
    isSplashActive: Boolean = false,
    miniPlayerTargetOffset: Offset? = null,
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (Long, String) -> Unit = { _, _ -> },
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    onNowPlayingOffsetYChange: (Float) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()

    val isScanning by remember(playerViewModel) {
        playerViewModel.playbackState.map { it.isScanInProgress }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val currentFolder by settingsViewModel.currentScanningFolder.collectAsState()
    val importError by viewModel.importError.collectAsState()

    val uiState = remember(
        playlists, songs, albums, artists,
        isScanning, currentFolder, importError
    ) {
        LibraryUiState(
            playlists = playlists,
            songs = songs,
            albums = albums,
            artists = artists,
            isScanning = isScanning,
            currentFolder = currentFolder,
            importError = importError
        )
    }

    LibraryContentV2(
        uiState = uiState,
        bottomPadding = bottomPadding,
        isSplashActive = isSplashActive,
        miniPlayerTargetOffset = miniPlayerTargetOffset,
        onDismissImportError = { viewModel.clearImportError() },
        onBulgeMiniPlayer = onBulgeMiniPlayer,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onOpenScanLocations = onOpenScanLocations,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onPlaylistClick = onPlaylistClick,
        onMiniPlayerCollapseChange = onMiniPlayerCollapseChange,
        onNowPlayingOffsetYChange = onNowPlayingOffsetYChange,
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
        },
        onCheckHasOfflineLyrics = { song ->
            viewModel.checkHasOfflineLyrics(song)
        }
    )
}

/**
 * Primary content composable taking a structured [LibraryUiState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContentV2(
    uiState: LibraryUiState,
    bottomPadding: Dp = 0.dp,
    isSplashActive: Boolean = false,
    miniPlayerTargetOffset: Offset? = null,
    onDismissImportError: () -> Unit = {},
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (Long, String) -> Unit = { _, _ -> },
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    onNowPlayingOffsetYChange: (Float) -> Unit = {},
    onPlaySong: (io.github.vinaygaykar.mp1.domain.models.Song, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> },
    onAddToQueue: (io.github.vinaygaykar.mp1.domain.models.Song) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylists: (Set<Long>) -> Unit = {},
    onImportPlaylist: (Uri) -> Unit = {},
    onAddSongsToPlaylist: (Long, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> },
    onCreatePlaylistWithSongs: (String, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> },
    onCheckHasOfflineLyrics: suspend (io.github.vinaygaykar.mp1.domain.models.Song) -> Boolean = { false }
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

    val importPlaylistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportPlaylist(it) }
    }

    val screenWidthPx = if (rootSize.width > 0) {
        rootSize.width.toFloat()
    } else {
        context.resources.displayMetrics.widthPixels.toFloat()
    }

    val splineBasedDecay = rememberSplineBasedDecay<Float>()

    val anchors = remember(screenWidthPx) {
        DraggableAnchors {
            DragValue.COLLAPSED at screenWidthPx
            DragValue.EXPANDED at 0f
        }
    }

    val anchoredDraggableState = rememberSaveable(
        saver = AnchoredDraggableState.Saver(
            snapAnimationSpec = tween(
                durationMillis = 350,
                easing = EaseOutCubic
            ),
            decayAnimationSpec = splineBasedDecay,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            confirmValueChange = { true }
        )
    ) {
        AnchoredDraggableState(
            initialValue = DragValue.EXPANDED,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(
                durationMillis = 350,
                easing = EaseOutCubic
            ),
            decayAnimationSpec = splineBasedDecay,
            confirmValueChange = { true }
        )
    }

    var savedCategory by rememberSaveable { mutableStateOf(LibraryCategory.SONGS) }

    val libraryState = remember(anchoredDraggableState) {
        LibraryState(anchoredDraggableState, initialCategory = savedCategory)
    }

    LaunchedEffect(libraryState.lastOpenedCategory) {
        savedCategory = libraryState.lastOpenedCategory
    }

    SideEffect {
        anchoredDraggableState.updateAnchors(anchors)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var previousOpenedCategory by remember { mutableStateOf<LibraryCategory?>(null) }

    val nestedScrollConnection = remember(onMiniPlayerCollapseChange, anchoredDraggableState, screenWidthPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                val dragOffsetPx = try {
                    anchoredDraggableState.requireOffset()
                } catch (_: Exception) { 0f }
                val currentDetailProgress = if (screenWidthPx > 0f) {
                    ((screenWidthPx - dragOffsetPx) / screenWidthPx).coerceIn(0f, 1f)
                } else 1f

                if (currentDetailProgress > 0.5f) {
                    if (deltaY < -8f) {
                        onMiniPlayerCollapseChange(true)
                    } else if (deltaY > 8f) {
                        onMiniPlayerCollapseChange(false)
                    }
                } else {
                    onMiniPlayerCollapseChange(true)
                }
                return Offset.Zero
            }
        }
    }

    fun openCategory(category: LibraryCategory) {
        if (category.isListCategory) {
            onMiniPlayerCollapseChange(false)
            val isNewCategory = previousOpenedCategory != category
            if (isNewCategory) {
                scrollBehavior.state.heightOffset = 0f
            }
            previousOpenedCategory = category
            libraryState.lastOpenedCategory = category
            coroutineScope.launch {
                libraryState.animateToExpanded()
            }
        } else if (category == LibraryCategory.SEARCH) {
            coroutineScope.launch {
                libraryState.animateToCollapsed()
            }
            onSearchClick()
        }
    }

    fun openCategoryMenu() {
        onMiniPlayerCollapseChange(true)
        coroutineScope.launch {
            libraryState.animateToCollapsed()
        }
    }

    val nowPlayingOffsetY = remember { Animatable(0f) }

    LaunchedEffect(nowPlayingOffsetY.value) {
        onNowPlayingOffsetYChange(nowPlayingOffsetY.value)
    }

    /**
     * Slides the collapsed Now Playing view downward below the screen bounds.
     */
    fun hideNowPlayingView() {
        coroutineScope.launch {
            val slideDistancePx = with(density) { (72.dp + bottomPadding).toPx() }
            nowPlayingOffsetY.animateTo(
                targetValue = slideDistancePx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    /**
     * Slides the collapsed Now Playing view back up into its original screen position.
     */
    fun showNowPlayingView() {
        coroutineScope.launch {
            nowPlayingOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    BackHandler(
        enabled = libraryState.isSelectionMode ||
                libraryState.isPlaylistSelectionMode ||
                anchoredDraggableState.currentValue == DragValue.EXPANDED
    ) {
        if (libraryState.isSelectionMode) {
            libraryState.clearSongSelection()
        } else if (libraryState.isPlaylistSelectionMode) {
            libraryState.clearPlaylistSelection()
        } else {
            openCategoryMenu()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
            .onGloballyPositioned { coords ->
                rootOffsetInWindow = coords.positionInRoot()
                rootSize = coords.size
            }
    ) {
        val targetX = if (miniPlayerTargetOffset != null) {
            miniPlayerTargetOffset.x - rootOffsetInWindow.x
        } else {
            rootSize.width.toFloat() - with(density) { (8.dp + 20.dp + 56.dp).toPx() }
        }

        val targetY = if (miniPlayerTargetOffset != null) {
            miniPlayerTargetOffset.y - rootOffsetInWindow.y
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

        val dragOffset = try {
            anchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            if (anchoredDraggableState.currentValue == DragValue.EXPANDED) 0f else screenWidthPx
        }

        val detailProgress = if (screenWidthPx > 0f) {
            ((screenWidthPx - dragOffset) / screenWidthPx).coerceIn(0f, 1f)
        } else {
            if (anchoredDraggableState.currentValue == DragValue.EXPANDED) 1f else 0f
        }

        val displayFolder = remember(uiState.currentFolder) {
            uiState.currentFolder
                ?.removePrefix("primary:")
                ?.removePrefix("primary")
                ?.trim()
        }

        LaunchedEffect(detailProgress) {
            if (detailProgress < 0.1f) {
                onMiniPlayerCollapseChange(true)
            } else if (detailProgress > 0.9f) {
                onMiniPlayerCollapseChange(false)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Horizontal,
                    enabled = !libraryState.isSelectionMode && !libraryState.isPlaylistSelectionMode
                )
        ) {
            // LEVEL 1: Category Menu Screen
            CategoryMenuScreen(
                uiState = uiState,
                displayFolder = displayFolder,
                bottomPadding = bottomPadding,
                detailProgress = detailProgress,
                screenWidthPx = screenWidthPx,
                onOpenCategory = { openCategory(it) },
                onSettingsClick = onSettingsClick,
                onOpenScanLocations = onOpenScanLocations
            )

            // LEVEL 2: Detail List Screen
            CategoryDetailScreen(
                uiState = uiState,
                libraryState = libraryState,
                displayFolder = displayFolder,
                bottomPadding = bottomPadding,
                detailProgress = detailProgress,
                isSplashActive = isSplashActive,
                scrollBehavior = scrollBehavior,
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onOpenScanLocations = onOpenScanLocations,
                onHideNowPlayingView = { hideNowPlayingView() },
                onShowNowPlayingView = { showNowPlayingView() },
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onPlaylistClick = onPlaylistClick,
                onPlaySong = onPlaySong,
                onAddToQueue = onAddToQueue,
                onCheckHasOfflineLyrics = onCheckHasOfflineLyrics,
                onImportPlaylistLauncher = {
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
                }
            )
        }

        // Dialogs & Bottom Sheets
        LibraryDialogsAndSheets(
            uiState = uiState,
            libraryState = libraryState,
            onDismissImportError = onDismissImportError,
            onCreatePlaylist = onCreatePlaylist,
            onDeletePlaylists = onDeletePlaylists,
            onAddSongsToPlaylist = onAddSongsToPlaylist,
            onCreatePlaylistWithSongs = onCreatePlaylistWithSongs
        )
    }
}

