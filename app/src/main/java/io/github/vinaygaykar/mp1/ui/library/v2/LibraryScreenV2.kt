package io.github.vinaygaykar.mp1.ui.library.v2

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt

@Composable
fun LibraryScreenV2(
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
    val useCleanAlbumGrid by viewModel.useCleanAlbumGrid.collectAsState()

    val isScanning by remember(playerViewModel) {
        playerViewModel.playbackState.map { it.isScanInProgress }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val currentFolder by settingsViewModel.currentScanningFolder.collectAsState()
    val importError by viewModel.importError.collectAsState()

    val uiState = remember(
        playlists, songs, albums, artists,
        useCleanAlbumGrid, isScanning, currentFolder, importError
    ) {
        LibraryUiState(
            playlists = playlists,
            songs = songs,
            albums = albums,
            artists = artists,
            useCleanAlbumGrid = useCleanAlbumGrid,
            isScanning = isScanning,
            currentFolder = currentFolder,
            importError = importError
        )
    }

    LibraryContentV2(
        uiState = uiState,
        bottomPadding = bottomPadding,
        isPlayerActive = isPlayerActive,
        miniPlayerTargetOffset = miniPlayerTargetOffset,
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

/**
 * Primary content composable taking a structured [LibraryUiState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContentV2(
    uiState: LibraryUiState,
    bottomPadding: Dp = 0.dp,
    isPlayerActive: Boolean = false,
    miniPlayerTargetOffset: Offset? = null,
    onDismissImportError: () -> Unit = {},
    onBulgeMiniPlayer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenScanLocations: () -> Unit = {},
    onPlaySong: (io.github.vinaygaykar.mp1.domain.models.Song, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> },
    onAddToQueue: (io.github.vinaygaykar.mp1.domain.models.Song) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylists: (Set<Long>) -> Unit = {},
    onImportPlaylist: (Uri) -> Unit = {},
    onAddSongsToPlaylist: (Long, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> },
    onCreatePlaylistWithSongs: (String, List<io.github.vinaygaykar.mp1.domain.models.Song>) -> Unit = { _, _ -> }
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
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
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
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            decayAnimationSpec = splineBasedDecay,
            confirmValueChange = { true }
        )
    }

    val libraryState = remember(anchoredDraggableState) {
        LibraryState(anchoredDraggableState)
    }

    SideEffect {
        anchoredDraggableState.updateAnchors(anchors)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var previousOpenedCategory by remember { mutableStateOf<LibraryCategory?>(null) }

    fun openCategory(category: LibraryCategory) {
        if (category.isListCategory) {
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
        coroutineScope.launch {
            libraryState.animateToCollapsed()
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

        var level1TitleBoundsMap by remember { mutableStateOf(mapOf<LibraryCategory, Rect>()) }
        var level2TitleBoundsMap by remember { mutableStateOf(mapOf<LibraryCategory, Rect>()) }
        var isActionBarExpanded by remember { mutableStateOf(true) }
        var shouldAnimateTransition by remember { mutableStateOf(true) }

        LaunchedEffect(detailProgress) {
            if (detailProgress == 0f || detailProgress == 1f) {
                shouldAnimateTransition = scrollBehavior.state.collapsedFraction <= 0.05f
            }
        }

        val effectiveActionBarExpanded = isActionBarExpanded && shouldAnimateTransition

        val sharedCategories = remember {
            setOf(
                LibraryCategory.PLAYLISTS,
                LibraryCategory.SONGS,
                LibraryCategory.ALBUMS,
                LibraryCategory.ARTISTS
            )
        }

        val categoryTitleCardModifiers = remember(
            libraryState.lastOpenedCategory,
            detailProgress,
            rootOffsetInWindow,
            screenWidthPx,
            effectiveActionBarExpanded
        ) {
            sharedCategories.associateWith { category ->
                Modifier
                    .graphicsLayer {
                        alpha =
                            if (libraryState.lastOpenedCategory == category && detailProgress > 0f && effectiveActionBarExpanded) 0f else 1f
                    }
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            val windowOffset = coords.positionInRoot()
                            val localOffset = windowOffset - rootOffsetInWindow
                            val unshiftedX = localOffset.x + (detailProgress * screenWidthPx / 3f)
                            val rect = Rect(
                                offset = Offset(unshiftedX, localOffset.y),
                                size = Size(
                                    coords.size.width.toFloat(),
                                    coords.size.height.toFloat()
                                )
                            )
                            if (level1TitleBoundsMap[category] != rect) {
                                level1TitleBoundsMap = level1TitleBoundsMap + (category to rect)
                            }
                        }
                    }
            }
        }

        val currentCategory = libraryState.lastOpenedCategory
        val isCurrentCategoryShared = currentCategory in sharedCategories

        val categoryHeaderTitleModifier = Modifier
            .graphicsLayer {
                alpha =
                    if (isCurrentCategoryShared && detailProgress < 1f && effectiveActionBarExpanded) 0f else 1f
            }
            .onGloballyPositioned { coords ->
                if (coords.isAttached && isCurrentCategoryShared) {
                    val windowOffset = coords.positionInRoot()
                    val localOffset = windowOffset - rootOffsetInWindow
                    val unshiftedX = localOffset.x - dragOffset
                    val rect = Rect(
                        offset = Offset(unshiftedX, localOffset.y),
                        size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                    )
                    if (level2TitleBoundsMap[currentCategory] != rect) {
                        level2TitleBoundsMap = level2TitleBoundsMap + (currentCategory to rect)
                    }
                }
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                onOpenScanLocations = onOpenScanLocations,
                categoryTitleModifiers = categoryTitleCardModifiers
            )

            // LEVEL 2: Detail List Screen
            CategoryDetailScreen(
                uiState = uiState,
                libraryState = libraryState,
                displayFolder = displayFolder,
                bottomPadding = bottomPadding,
                dragOffset = dragOffset,
                isPlayerActive = isPlayerActive,
                scrollBehavior = scrollBehavior,
                onOpenCategoryMenu = { openCategoryMenu() },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                onOpenScanLocations = onOpenScanLocations,
                onActionBarExpandedChange = { isActionBarExpanded = it },
                onPlaySong = onPlaySong,
                onAddToQueue = onAddToQueue,
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
                },
                onAddFlyingArtwork = { flyingArtworks.add(it) },
                categoryHeaderTitleModifier = categoryHeaderTitleModifier
            )
        }

        // Shared Element Overlay for title transition (Songs & Albums)
        val l1Bounds = level1TitleBoundsMap[currentCategory]
        val l2Bounds = level2TitleBoundsMap[currentCategory]
        if (isCurrentCategoryShared &&
            l1Bounds != null &&
            l2Bounds != null &&
            detailProgress > 0f &&
            detailProgress < 1f &&
            effectiveActionBarExpanded
        ) {
            val currentX = lerp(l1Bounds.left, l2Bounds.left, detailProgress)
            val currentY = lerp(l1Bounds.top, l2Bounds.top, detailProgress)
            val currentWidth = lerp(l1Bounds.width, l2Bounds.width, detailProgress)
            val currentHeight = lerp(l1Bounds.height, l2Bounds.height, detailProgress)

            val startFontSize = MaterialTheme.typography.displaySmall.fontSize
            val endFontSize = MaterialTheme.typography.titleLarge.fontSize
            val currentFontSize = lerp(startFontSize.value, endFontSize.value, detailProgress).sp

            val currentColor = lerp(
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.onSurface,
                detailProgress
            )
            val currentWeightInt = lerp(
                FontWeight.Bold.weight,
                FontWeight.Bold.weight,
                detailProgress
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(
                        width = with(density) { currentWidth.toDp() },
                        height = with(density) { currentHeight.toDp() }
                    )
            ) {
                Text(
                    text = currentCategory.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = currentFontSize,
                        fontWeight = FontWeight(currentWeightInt)
                    ),
                    color = currentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                )
            }
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
