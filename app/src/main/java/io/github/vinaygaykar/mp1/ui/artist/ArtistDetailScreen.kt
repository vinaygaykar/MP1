@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.vinaygaykar.mp1.ui.artist

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.vinaygaykar.mp1.data.network.models.DeezerArtist
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.components.LocalAnimatedVisibilityScope
import io.github.vinaygaykar.mp1.ui.components.SongHeroOverlayContainer
import io.github.vinaygaykar.mp1.ui.components.SongListItem
import io.github.vinaygaykar.mp1.ui.components.SongOverlayData
import io.github.vinaygaykar.mp1.ui.components.artistSharedElement
import io.github.vinaygaykar.mp1.ui.library.SongsMultiSelectOverlay
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.ui.theme.isArtistMatchingPlayingTrack
import io.github.vinaygaykar.mp1.ui.theme.rememberMorphingArtistShape

@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String, String?) -> Unit,
    onHideNowPlayingView: () -> Unit = {},
    onShowNowPlayingView: () -> Unit = {},
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val artist by viewModel.artist.collectAsState()
    val artistImageUrl by viewModel.artistImageUrl.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val featuredAlbums by viewModel.featuredAlbums.collectAsState()
    val collaborators by viewModel.collaborators.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val candidateImages by viewModel.candidateImages.collectAsState()
    val isSearchingImages by viewModel.isSearchingImages.collectAsState()

    ArtistDetailContent(
        artist = artist,
        artistImageUrl = artistImageUrl,
        albums = albums,
        featuredAlbums = featuredAlbums,
        collaborators = collaborators,
        songs = songs,
        playbackState = playbackState,
        playlists = playlists,
        candidateImages = candidateImages,
        isSearchingImages = isSearchingImages,
        artistId = viewModel.artistId,
        onBack = onBack,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onHideNowPlayingView = onHideNowPlayingView,
        onShowNowPlayingView = onShowNowPlayingView,
        onMiniPlayerCollapseChange = onMiniPlayerCollapseChange,
        onPlaySong = { viewModel.playSong(it) },
        onPlayNext = { viewModel.playNext(it) },
        onAddToQueue = { viewModel.addToQueue(it) },
        onAddSongsToQueue = { viewModel.addToQueue(it) },
        onAddSongsToPlaylist = { playlistId, songList -> viewModel.addSongsToPlaylist(playlistId, songList) },
        onCreatePlaylistAndAddSongs = { name, songList -> viewModel.createPlaylistAndAddSongs(name, songList) },
        onSearchArtistImages = { query -> viewModel.searchArtistImages(query) },
        onSelectArtistImage = { selectedUrl -> viewModel.selectArtistImage(selectedUrl) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ArtistDetailContent(
    artist: Artist?,
    artistImageUrl: String?,
    albums: List<Album>,
    featuredAlbums: List<Album>,
    collaborators: List<Artist>,
    songs: List<Song>,
    playbackState: PlaybackUiState,
    playlists: List<Playlist>,
    candidateImages: List<DeezerArtist> = emptyList(),
    isSearchingImages: Boolean = false,
    artistId: String = "",
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String, String?) -> Unit,
    onHideNowPlayingView: () -> Unit = {},
    onShowNowPlayingView: () -> Unit = {},
    onMiniPlayerCollapseChange: (Boolean) -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    onPlayNext: (List<Song>) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddSongsToQueue: (List<Song>) -> Unit = {},
    onAddSongsToPlaylist: (Long, List<Song>) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddSongs: (String, List<Song>) -> Unit = { _, _ -> },
    onSearchArtistImages: (String) -> Unit = {},
    onSelectArtistImage: (String) -> Unit = {},
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

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showSearchImageDialog by remember { mutableStateOf(false) }

    val sortedAlbums = remember(albums) {
        albums.sortedWith(
            compareByDescending<Album> { it.year ?: 0 }
                .thenBy { it.name.lowercase() }
        )
    }

    val sortedFeaturedAlbums = remember(featuredAlbums) {
        featuredAlbums.sortedWith(
            compareByDescending<Album> { it.year ?: 0 }
                .thenBy { it.name.lowercase() }
        )
    }

    val discographyGroups = remember(songs, albums, featuredAlbums) {
        val knownAlbumsMap = (albums + featuredAlbums).associateBy { it.name.lowercase() }
        songs.groupBy { it.album }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<Song>>> { (albumName, _) ->
                    knownAlbumsMap[albumName.lowercase()]?.year ?: 0
                }.thenBy { (albumName, _) ->
                    albumName.lowercase()
                }
            )
            .map { (albumName, albumSongs) ->
                Triple(albumName, knownAlbumsMap[albumName.lowercase()], albumSongs)
            }
    }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var artistSeedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(artistImageUrl) {
        artistSeedColor = if (!artistImageUrl.isNullOrEmpty()) {
            DynamicThemeHelper.extractSeedColor(context, artistImageUrl)
        } else {
            null
        }
    }

    val artistColorScheme = remember(artistSeedColor, isDark) {
        artistSeedColor?.let { seed ->
            DynamicThemeHelper.createDynamicColorScheme(seed, isDark)
        }
    }

    val currentColorScheme = artistColorScheme ?: MaterialTheme.colorScheme
    val isPlayerActive = playbackState.trackTitle.isNotEmpty()

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

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

    MaterialTheme(colorScheme = currentColorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                onAddToQueue = { song -> onAddToQueue(song) },
                onAddToPlaylist = { song ->
                    selectedSongIds = setOf(song.id)
                    showPlaylistSelectionSheet = true
                },
                onAlbumClick = onAlbumClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    key(artistSeedColor) {
                        when {
                            isWideWindow -> {
                                ArtistDetailLandscapeContent(
                                    artist = artist,
                                    artistImageUrl = artistImageUrl,
                                    collaborators = collaborators,
                                    songs = songs,
                                    playbackState = playbackState,
                                    artistId = artistId,
                                    sortedAlbums = sortedAlbums,
                                    sortedFeaturedAlbums = sortedFeaturedAlbums,
                                    discographyGroups = discographyGroups,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    isActionPopupOpen = isActionPopupOpen,
                                    onToggleSongSelection = { toggleSongSelection(it) },
                                    onActiveOverlayChange = { activeOverlayData = it },
                                    onPreviewImageUrlChange = { previewImageUrl = it },
                                    onShowSearchImageDialogChange = { showSearchImageDialog = it },
                                    onBack = onBack,
                                    onAlbumClick = onAlbumClick,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    onMiniPlayerCollapseChange = onMiniPlayerCollapseChange
                                )
                            }
                            isCompactHeight -> {
                                ArtistDetailCompactContent(
                                    artist = artist,
                                    artistImageUrl = artistImageUrl,
                                    collaborators = collaborators,
                                    songs = songs,
                                    playbackState = playbackState,
                                    artistId = artistId,
                                    sortedAlbums = sortedAlbums,
                                    sortedFeaturedAlbums = sortedFeaturedAlbums,
                                    discographyGroups = discographyGroups,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    isActionPopupOpen = isActionPopupOpen,
                                    onToggleSongSelection = { toggleSongSelection(it) },
                                    onActiveOverlayChange = { activeOverlayData = it },
                                    onPreviewImageUrlChange = { previewImageUrl = it },
                                    onShowSearchImageDialogChange = { showSearchImageDialog = it },
                                    onBack = onBack,
                                    onAlbumClick = onAlbumClick,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    onMiniPlayerCollapseChange = onMiniPlayerCollapseChange
                                )
                            }
                            else -> {
                                ArtistDetailPortraitContent(
                                    artist = artist,
                                    artistImageUrl = artistImageUrl,
                                    collaborators = collaborators,
                                    songs = songs,
                                    playbackState = playbackState,
                                    artistId = artistId,
                                    sortedAlbums = sortedAlbums,
                                    sortedFeaturedAlbums = sortedFeaturedAlbums,
                                    discographyGroups = discographyGroups,
                                    hazeState = hazeState,
                                    selectedSongIds = selectedSongIds,
                                    isSelectionMode = isSelectionMode,
                                    isActionPopupOpen = isActionPopupOpen,
                                    onToggleSongSelection = { toggleSongSelection(it) },
                                    onActiveOverlayChange = { activeOverlayData = it },
                                    onPreviewImageUrlChange = { previewImageUrl = it },
                                    onShowSearchImageDialogChange = { showSearchImageDialog = it },
                                    onBack = onBack,
                                    onAlbumClick = onAlbumClick,
                                    onArtistClick = onArtistClick,
                                    onPlaySong = onPlaySong,
                                    onMiniPlayerCollapseChange = onMiniPlayerCollapseChange
                                )
                            }
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

            // Full Screen Image Preview Overlay
            if (!previewImageUrl.isNullOrEmpty()) {
                Dialog(
                    onDismissRequest = { previewImageUrl = null },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.92f))
                            .clickable { previewImageUrl = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = previewImageUrl,
                                contentDescription = artist?.name ?: "Artist Image Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    previewImageUrl = null
                                    showSearchImageDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Image")
                            }
                        }

                        IconButton(
                            onClick = { previewImageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 40.dp, end = 16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Image Search & Selection Dialog
            if (showSearchImageDialog) {
                ArtistImageSearchDialog(
                    initialQuery = artist?.name ?: "",
                    candidateImages = candidateImages,
                    isSearching = isSearchingImages,
                    onSearch = { query -> onSearchArtistImages(query) },
                    onSelectImage = { selectedUrl ->
                        onSelectArtistImage(selectedUrl)
                        showSearchImageDialog = false
                    },
                    onDismiss = { showSearchImageDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailPortraitContent(
    artist: Artist?,
    artistImageUrl: String?,
    collaborators: List<Artist>,
    songs: List<Song>,
    playbackState: PlaybackUiState,
    artistId: String,
    sortedAlbums: List<Album>,
    sortedFeaturedAlbums: List<Album>,
    discographyGroups: List<Triple<String, Album?, List<Song>>>,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    isActionPopupOpen: Boolean,
    onToggleSongSelection: (Song) -> Unit,
    onActiveOverlayChange: (SongOverlayData?) -> Unit,
    onPreviewImageUrlChange: (String?) -> Unit,
    onShowSearchImageDialogChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String, String?) -> Unit,
    onPlaySong: (Song) -> Unit,
    onMiniPlayerCollapseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isPlayerActive = playbackState.trackTitle.isNotEmpty()
    val bottomPadding = if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

    val lazyListState = rememberLazyListState()

    var miniPlayerBulgeTrigger by remember { mutableLongStateOf(0L) }
    var miniPlayerTargetOffset by remember { mutableStateOf<Offset?>(null) }

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
        modifier = modifier
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

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(
                top = statusBarTop + 64.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist Header Image & Name Redesign with AutoResizedText
            item {
                val displayedArtistName = artist?.name ?: "Loading Artist..."
                val currentPlayingArtist = playbackState.artist
                val currentPlayingAlbum = playbackState.album

                val isMatchingArtist = remember(
                    displayedArtistName,
                    currentPlayingArtist,
                    currentPlayingAlbum
                ) {
                    isArtistMatchingPlayingTrack(
                        artistName = displayedArtistName,
                        playingArtistString = currentPlayingArtist,
                        playingAlbumString = currentPlayingAlbum
                    )
                }

                val isMorphing = playbackState.isPlaying && isMatchingArtist

                val artistShape = rememberMorphingArtistShape(
                    artistId = artist?.id ?: "",
                    artistName = artist?.name ?: "",
                    isMorphing = isMorphing
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArtistHeaderWithAutoResizedName(
                        artistName = displayedArtistName,
                        imageContent = { imageModifier ->
                            Box(
                                modifier = imageModifier
                                    .artistSharedElement(key = "artist_image_${artist?.id ?: artistId}")
                                    .clip(artistShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        if (!artistImageUrl.isNullOrEmpty()) {
                                            onPreviewImageUrlChange(artistImageUrl)
                                        } else {
                                            onShowSearchImageDialogChange(true)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val personPlaceholder = rememberVectorPainter(Icons.Default.Person)
                                val detailImageRequest = remember(artistImageUrl) {
                                    ImageRequest.Builder(context)
                                        .data(artistImageUrl)
                                        .apply {
                                            if (!artistImageUrl.isNullOrEmpty()) {
                                                placeholderMemoryCacheKey(artistImageUrl)
                                            }
                                        }
                                        .crossfade(false)
                                        .build()
                                }

                                AsyncImage(
                                    model = detailImageRequest,
                                    contentDescription = displayedArtistName,
                                    error = personPlaceholder,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Albums Horizontal List
            if (sortedAlbums.isNotEmpty()) {
                item {
                    AlbumsSection(
                        title = "Albums",
                        albums = sortedAlbums,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            // Featured On Horizontal List
            if (sortedFeaturedAlbums.isNotEmpty()) {
                item {
                    AlbumsSection(
                        title = "Features",
                        albums = sortedFeaturedAlbums,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            // Discography (Songs grouped by Album)
            if (discographyGroups.isNotEmpty()) {
                item(key = "discography_header") {
                    DiscographyHeaderWithSongCount(
                        songCount = songs.size
                    )
                }

                discographyGroups.forEach { (albumName, matchingAlbum, albumSongs) ->
                    item(key = "discography_album_header_$albumName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 12.dp,
                                    bottom = 6.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val albumArtwork = matchingAlbum?.artworkUri
                                ?: albumSongs.firstOrNull { it.artworkUri != null }?.artworkUri
                            if (albumArtwork != null) {
                                val request = remember(albumArtwork) {
                                    ImageRequest.Builder(context)
                                        .data(albumArtwork)
                                        .size(150, 150)
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = albumName,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val yearStr = matchingAlbum?.year?.takeIf { it > 0 }?.toString()
                                val trackCountStr =
                                    "${albumSongs.size} ${if (albumSongs.size == 1) "song" else "tracks"}"
                                val subtitle = listOfNotNull(yearStr, trackCountStr).joinToString(" • ")
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    itemsIndexed(albumSongs, key = { _, song -> song.id }) { _, song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        SongListItem(
                            song = song,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSongSelection(song)
                                } else {
                                    onPlaySong(song)
                                }
                            },
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            showTrackNumber = true,
                            onLongClick = { onToggleSongSelection(song) },
                            onMoreClick = { offset, size ->
                                onActiveOverlayChange(
                                    SongOverlayData(
                                        song = song,
                                        startOffset = offset,
                                        startSize = size
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Collaborators Horizontal List (after songs list)
            if (collaborators.isNotEmpty()) {
                item {
                    CollaboratorsSection(
                        title = if (collaborators.size == 1) "Featured Artist" else "Featured Artists",
                        collaborators = collaborators,
                        playbackState = playbackState,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        // Floating Back Arrow
        if (!isActionPopupOpen) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarTop + 8.dp, start = 16.dp)
                    .zIndex(3f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailLandscapeContent(
    artist: Artist?,
    artistImageUrl: String?,
    collaborators: List<Artist>,
    songs: List<Song>,
    playbackState: PlaybackUiState,
    artistId: String,
    sortedAlbums: List<Album>,
    sortedFeaturedAlbums: List<Album>,
    discographyGroups: List<Triple<String, Album?, List<Song>>>,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    isActionPopupOpen: Boolean,
    onToggleSongSelection: (Song) -> Unit,
    onActiveOverlayChange: (SongOverlayData?) -> Unit,
    onPreviewImageUrlChange: (String?) -> Unit,
    onShowSearchImageDialogChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String, String?) -> Unit,
    onPlaySong: (Song) -> Unit,
    onMiniPlayerCollapseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val safeStart = maxOf(safePadding.calculateLeftPadding(layoutDirection), 16.dp)
    val safeEnd = maxOf(safePadding.calculateRightPadding(layoutDirection), 16.dp)

    val isPlayerActive = playbackState.trackTitle.isNotEmpty()
    val bottomPadding = if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

    val middleLazyListState = rememberLazyListState()
    val rightLazyListState = rememberLazyListState()

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

    val displayedArtistName = artist?.name ?: "Loading Artist..."
    val currentPlayingArtist = playbackState.artist
    val currentPlayingAlbum = playbackState.album

    val isMatchingArtist = remember(displayedArtistName, currentPlayingArtist, currentPlayingAlbum) {
        isArtistMatchingPlayingTrack(
            artistName = displayedArtistName,
            playingArtistString = currentPlayingArtist,
            playingAlbumString = currentPlayingAlbum
        )
    }

    val isMorphing = playbackState.isPlaying && isMatchingArtist
    val artistShape = rememberMorphingArtistShape(
        artistId = artist?.id ?: "",
        artistName = artist?.name ?: "",
        isMorphing = isMorphing
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // PANE 1 (Leftmost Pane): Non-scrollable Back Button pane
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .padding(
                        top = statusBarTop + 8.dp,
                        start = safeStart,
                        end = 8.dp,
                        bottom = bottomPadding
                    ),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isActionPopupOpen) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.zIndex(3f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            }

            // PANE 2 (Middle Pane): Scrollable Artist Name & Image component + Song Count
            LazyColumn(
                state = middleLazyListState,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f)
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    top = statusBarTop + 16.dp,
                    start = 8.dp,
                    end = 12.dp,
                    bottom = bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .artistSharedElement(key = "artist_image_${artist?.id ?: artistId}")
                                .clip(artistShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    if (!artistImageUrl.isNullOrEmpty()) {
                                        onPreviewImageUrlChange(artistImageUrl)
                                    } else {
                                        onShowSearchImageDialogChange(true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val personPlaceholder = rememberVectorPainter(Icons.Default.Person)
                            val detailImageRequest = remember(artistImageUrl) {
                                ImageRequest.Builder(context)
                                    .data(artistImageUrl)
                                    .apply {
                                        if (!artistImageUrl.isNullOrEmpty()) {
                                            placeholderMemoryCacheKey(artistImageUrl)
                                        }
                                    }
                                    .crossfade(false)
                                    .build()
                            }

                            AsyncImage(
                                model = detailImageRequest,
                                contentDescription = displayedArtistName,
                                error = personPlaceholder,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AutoResizedText(
                            text = displayedArtistName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            minFontSize = 16.sp,
                            maxFontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // PANE 3 (Right Pane): Albums, Featured Albums, Featured Artists/Collaborators, Discography
            LazyColumn(
                state = rightLazyListState,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.65f)
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    top = statusBarTop + 16.dp,
                    start = 12.dp,
                    end = safeEnd,
                    bottom = bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Albums Horizontal List
                if (sortedAlbums.isNotEmpty()) {
                    item {
                        AlbumsSection(
                            title = "Albums",
                            albums = sortedAlbums,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }

                // 2. Featured Albums Horizontal List
                if (sortedFeaturedAlbums.isNotEmpty()) {
                    item {
                        AlbumsSection(
                            title = "Features",
                            albums = sortedFeaturedAlbums,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }

                // 3. Featured Artists / Collaborators Horizontal List
                if (collaborators.isNotEmpty()) {
                    item {
                        CollaboratorsSection(
                            title = if (collaborators.size == 1) "Featured Artist" else "Featured Artists",
                            collaborators = collaborators,
                            playbackState = playbackState,
                            onArtistClick = onArtistClick
                        )
                    }
                }

                // 4. Discography Header & Grouped Songs
                item {
                    DiscographyHeaderWithSongCount(
                        songCount = songs.size
                    )
                }

                discographyGroups.forEach { (albumName, matchingAlbum, albumSongs) ->
                    item(key = "discography_album_header_$albumName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 12.dp,
                                    bottom = 6.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val albumArtwork = matchingAlbum?.artworkUri
                                ?: albumSongs.firstOrNull { it.artworkUri != null }?.artworkUri
                            if (albumArtwork != null) {
                                val request = remember(albumArtwork) {
                                    ImageRequest.Builder(context)
                                        .data(albumArtwork)
                                        .size(150, 150)
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = albumName,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val yearStr = matchingAlbum?.year?.takeIf { it > 0 }?.toString()
                                val trackCountStr =
                                    "${albumSongs.size} ${if (albumSongs.size == 1) "song" else "tracks"}"
                                val subtitle = listOfNotNull(yearStr, trackCountStr).joinToString(" • ")
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    itemsIndexed(albumSongs, key = { _, song -> song.id }) { _, song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        SongListItem(
                            song = song,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSongSelection(song)
                                } else {
                                    onPlaySong(song)
                                }
                            },
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            showTrackNumber = true,
                            onLongClick = { onToggleSongSelection(song) },
                            onMoreClick = { offset, size ->
                                onActiveOverlayChange(
                                    SongOverlayData(
                                        song = song,
                                        startOffset = offset,
                                        startSize = size
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailCompactContent(
    artist: Artist?,
    artistImageUrl: String?,
    collaborators: List<Artist>,
    songs: List<Song>,
    playbackState: PlaybackUiState,
    artistId: String,
    sortedAlbums: List<Album>,
    sortedFeaturedAlbums: List<Album>,
    discographyGroups: List<Triple<String, Album?, List<Song>>>,
    hazeState: HazeState,
    selectedSongIds: Set<String>,
    isSelectionMode: Boolean,
    isActionPopupOpen: Boolean,
    onToggleSongSelection: (Song) -> Unit,
    onActiveOverlayChange: (SongOverlayData?) -> Unit,
    onPreviewImageUrlChange: (String?) -> Unit,
    onShowSearchImageDialogChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String, String?) -> Unit,
    onPlaySong: (Song) -> Unit,
    onMiniPlayerCollapseChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isPlayerActive = playbackState.trackTitle.isNotEmpty()
    val bottomPadding = if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

    val lazyListState = rememberLazyListState()
    val displayedArtistName = artist?.name ?: "Loading Artist..."

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Item: Full-width artist image background with artist name at the bottom
            item {
                ArtistCompactHeader(
                    artistName = displayedArtistName,
                    artistImageUrl = artistImageUrl,
                    artistId = artistId,
                    onImageClick = {
                        if (!artistImageUrl.isNullOrEmpty()) {
                            onPreviewImageUrlChange(artistImageUrl)
                        } else {
                            onShowSearchImageDialogChange(true)
                        }
                    }
                )
            }

            // Albums Horizontal List
            if (sortedAlbums.isNotEmpty()) {
                item {
                    AlbumsSection(
                        title = "Albums",
                        albums = sortedAlbums,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            // Featured Albums Horizontal List
            if (sortedFeaturedAlbums.isNotEmpty()) {
                item {
                    AlbumsSection(
                        title = "Features",
                        albums = sortedFeaturedAlbums,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

            // Discography (Songs grouped by Album)
            if (discographyGroups.isNotEmpty()) {
                item(key = "discography_header") {
                    DiscographyHeaderWithSongCount(
                        songCount = songs.size
                    )
                }

                discographyGroups.forEach { (albumName, matchingAlbum, albumSongs) ->
                    item(key = "discography_album_header_$albumName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 12.dp,
                                    bottom = 6.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val albumArtwork = matchingAlbum?.artworkUri
                                ?: albumSongs.firstOrNull { it.artworkUri != null }?.artworkUri
                            if (albumArtwork != null) {
                                val request = remember(albumArtwork) {
                                    ImageRequest.Builder(context)
                                        .data(albumArtwork)
                                        .size(150, 150)
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = albumName,
                                    modifier = Modifier.size(52.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val yearStr = matchingAlbum?.year?.takeIf { it > 0 }?.toString()
                                val trackCountStr =
                                    "${albumSongs.size} ${if (albumSongs.size == 1) "song" else "tracks"}"
                                val subtitle = listOfNotNull(yearStr, trackCountStr).joinToString(" • ")
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    itemsIndexed(albumSongs, key = { _, song -> song.id }) { _, song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        SongListItem(
                            song = song,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSongSelection(song)
                                } else {
                                    onPlaySong(song)
                                }
                            },
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            showTrackNumber = true,
                            onLongClick = { onToggleSongSelection(song) },
                            onMoreClick = { offset, size ->
                                onActiveOverlayChange(
                                    SongOverlayData(
                                        song = song,
                                        startOffset = offset,
                                        startSize = size
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Collaborators Horizontal List
            if (collaborators.isNotEmpty()) {
                item {
                    CollaboratorsSection(
                        title = if (collaborators.size == 1) "Featured Artist" else "Featured Artists",
                        collaborators = collaborators,
                        playbackState = playbackState,
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        // Floating Back Arrow in top left
        if (!isActionPopupOpen) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarTop + 8.dp, start = 16.dp)
                    .zIndex(3f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
}

@Composable
private fun AlbumsSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(albums, key = { it.id }) { album ->
                ArtistAlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.id) }
                )
            }
        }
    }
}

@Composable
private fun CollaboratorsSection(
    title: String,
    collaborators: List<Artist>,
    playbackState: PlaybackUiState,
    onArtistClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (collaborators.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(collaborators, key = { it.id }) { collaborator ->
                CollaboratorArtistCard(
                    artist = collaborator,
                    playingArtist = playbackState.artist,
                    playingAlbum = playbackState.album,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onArtistClick(collaborator.id, collaborator.imageUrl) }
                )
            }
        }
    }
}

@Composable
private fun ArtistCompactHeader(
    artistName: String,
    artistImageUrl: String?,
    artistId: String = "",
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .artistSharedElement(key = "artist_image_${artistId}")
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onImageClick() },
        contentAlignment = Alignment.BottomStart
    ) {
        val personPlaceholder = rememberVectorPainter(Icons.Default.Person)
        val imageRequest = remember(artistImageUrl) {
            ImageRequest.Builder(context)
                .data(artistImageUrl)
                .apply {
                    if (!artistImageUrl.isNullOrEmpty()) {
                        placeholderMemoryCacheKey(artistImageUrl)
                    }
                }
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = artistName,
            error = personPlaceholder,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        AutoResizedText(
            text = artistName,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            minFontSize = 18.sp,
            maxFontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun DiscographyHeaderWithSongCount(
    songCount: Int,
    modifier: Modifier = Modifier
) {
    val countText = remember(songCount) {
        if (songCount == 1) "1 song" else "$songCount songs"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Discography",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = countText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -----------------------------------------------------------------------------
// PREVIEWS
// -----------------------------------------------------------------------------

private val samplePreviewArtist = Artist(
    id = "artist_1",
    name = "Lorem Ipsum Dolor Sit Amet",
    trackCount = 12
)

private val samplePreviewAlbums = listOf(
    Album(
        id = "album_1",
        name = "Dolor Sit Amet",
        artist = "Lorem Ipsum",
        year = 2023,
        artworkUri = null,
        trackCount = 6
    ),
    Album(
        id = "album_2",
        name = "Consectetur Adipiscing",
        artist = "Lorem Ipsum",
        year = 2021,
        artworkUri = null,
        trackCount = 6
    )
)

private val samplePreviewFeaturedAlbums = listOf(
    Album(
        id = "album_3",
        name = "Elit Sed Do",
        artist = "Eiusmod Tempor",
        year = 2022,
        artworkUri = null,
        trackCount = 10
    )
)

private val samplePreviewCollaborators = listOf(
    Artist(id = "artist_2", name = "Incididunt Ut", trackCount = 4),
    Artist(id = "artist_3", name = "Labore Et", trackCount = 2)
)

private val samplePreviewSongs = listOf(
    Song(
        id = "song_1",
        uri = Uri.EMPTY,
        title = "Magna Aliqua",
        artist = "Lorem Ipsum",
        album = "Dolor Sit Amet",
        durationMs = 210000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2023,
        artworkUri = null,
        fileName = "track1.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum"
    ),
    Song(
        id = "song_2",
        uri = Uri.EMPTY,
        title = "Ut Enim Ad Minim",
        artist = "Lorem Ipsum feat. Incididunt Ut",
        album = "Dolor Sit Amet",
        durationMs = 195000L,
        trackNumber = 2,
        discNumber = 1,
        year = 2023,
        artworkUri = null,
        fileName = "track2.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum, Incididunt Ut"
    ),
    Song(
        id = "song_3",
        uri = Uri.EMPTY,
        title = "Venia Quis Nostrud",
        artist = "Lorem Ipsum",
        album = "Consectetur Adipiscing",
        durationMs = 240000L,
        trackNumber = 1,
        discNumber = 1,
        year = 2021,
        artworkUri = null,
        fileName = "track3.mp3",
        lastModified = 0L,
        artistsDisplayString = "Lorem Ipsum"
    )
)

private val samplePreviewPlaybackState = PlaybackUiState(
    trackTitle = "Magna Aliqua",
    artist = "Lorem Ipsum",
    album = "Dolor Sit Amet",
    isPlaying = true
)

private val samplePreviewPlaylists = listOf(
    Playlist(id = 1, name = "Exercitation", songCount = 15),
    Playlist(id = 2, name = "Ullamco Laboris", songCount = 30)
)

@Preview(
    name = "Portrait Light",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "Artist Detail Screen",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ArtistDetailScreenPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ArtistDetailContent(
                artist = samplePreviewArtist,
                artistImageUrl = null,
                albums = samplePreviewAlbums,
                featuredAlbums = samplePreviewFeaturedAlbums,
                collaborators = samplePreviewCollaborators,
                songs = samplePreviewSongs,
                playbackState = samplePreviewPlaybackState,
                playlists = samplePreviewPlaylists,
                onBack = {},
                onAlbumClick = {},
                onArtistClick = { _, _ -> }
            )
        }
    }
}

@Composable
fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val artworkUri = album.artworkUri
            if (artworkUri != null) {
                val request = remember(artworkUri) {
                    ImageRequest.Builder(context)
                        .data(artworkUri)
                        .size(250, 250)
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
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        val yearString = album.year?.takeIf { it > 0 }?.toString() ?: ""
        if (yearString.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = yearString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CollaboratorArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playingArtist: String = "",
    playingAlbum: String = "",
    isPlaying: Boolean = false
) {
    val isMatchingArtist = remember(artist.name, playingArtist, playingAlbum) {
        isArtistMatchingPlayingTrack(
            artistName = artist.name,
            playingArtistString = playingArtist,
            playingAlbumString = playingAlbum
        )
    }

    val isMorphing = isPlaying && isMatchingArtist

    val collaboratorShape = rememberMorphingArtistShape(
        artistId = artist.id,
        artistName = artist.name,
        isMorphing = isMorphing
    )

    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .artistSharedElement(key = "artist_image_${artist.id}")
                .clip(collaboratorShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun ArtistHeaderWithAutoResizedName(
    artistName: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.secondary,
    imageContent: @Composable (Modifier) -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val line1MinFontSize = 44.sp
    val line1MaxFontSize = 56.sp
    val line1SingleLineMaxFontSize = 80.sp

    val line2MinFontSize = 14.sp
    val line2MaxFontSize = 44.sp

    val line1StyleAtMin = MaterialTheme.typography.headlineLarge.copy(
        fontSize = line1MinFontSize,
        fontWeight = FontWeight.Bold
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Edge-to-edge text width (minus 16.dp horizontal padding)
        val availableWidthPx = with(density) { (maxWidth - 16.dp).toPx() }.toInt()

        val (line1Text, line2Text) = remember(artistName, availableWidthPx, line1StyleAtMin) {
            if (artistName.isBlank() || availableWidthPx <= 0) return@remember Pair(artistName, "")

            // 1. Check if full name fits on Line 1 at line1MinFontSize
            val fullMeasure = textMeasurer.measure(
                text = AnnotatedString(artistName),
                style = line1StyleAtMin,
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = availableWidthPx)
            )

            val fitsOnSingleLineAtMin = !fullMeasure.hasVisualOverflow &&
                    !fullMeasure.didOverflowWidth &&
                    fullMeasure.lineCount == 1

            if (fitsOnSingleLineAtMin) {
                // All words fit on 1 line at line1MinFontSize
                Pair(artistName, "")
            } else {
                val words = artistName.split(" ")
                if (words.size > 1) {
                    // Imbalanced split search: start at k = words.size / 2 down to 1
                    val initialTarget = words.size / 2
                    var selectedCount = 1
                    for (k in initialTarget downTo 1) {
                        val candidate = words.take(k).joinToString(" ")
                        val candidateMeasure = textMeasurer.measure(
                            text = AnnotatedString(candidate),
                            style = line1StyleAtMin,
                            softWrap = false,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = availableWidthPx)
                        )
                        if (!candidateMeasure.hasVisualOverflow && !candidateMeasure.didOverflowWidth) {
                            selectedCount = k
                            break
                        }
                    }
                    val l1 = words.take(selectedCount).joinToString(" ")
                    val l2 = words.drop(selectedCount).joinToString(" ")
                    Pair(l1, l2)
                } else {
                    // Single long word that doesn't fit on Line 1 at line1MinFontSize
                    var bestLength = 1
                    for (i in 1..artistName.length) {
                        val candidate = artistName.substring(0, i)
                        val measure = textMeasurer.measure(
                            text = AnnotatedString(candidate),
                            style = line1StyleAtMin,
                            softWrap = false,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = availableWidthPx)
                        )
                        if (!measure.hasVisualOverflow && !measure.didOverflowWidth) {
                            bestLength = i
                        } else {
                            break
                        }
                    }
                    val l1 = artistName.substring(0, bestLength)
                    val l2 = artistName.substring(bestLength)
                    Pair(l1, l2)
                }
            }
        }

        val isSingleLine = line2Text.isBlank()

        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val isTransitionCompleted by remember(animatedVisibilityScope) {
            derivedStateOf {
                if (animatedVisibilityScope == null) {
                    true
                } else {
                    val transition = animatedVisibilityScope.transition
                    transition.currentState == transition.targetState && transition.targetState == EnterExitState.Visible
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // 1. Bottom text (Line 2) - Higher Z-index (2f) than image (1f) when 2 lines are present
            if (!isSingleLine && line2Text.isNotBlank()) {
                AnimatedVisibility(
                    visible = isTransitionCompleted,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> -fullHeight },
                        animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(durationMillis = 350)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset(y = 24.dp)
                        .zIndex(2f)
                ) {
                    AutoResizedText(
                        text = line2Text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = line2MaxFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        minFontSize = line2MinFontSize,
                        maxFontSize = line2MaxFontSize,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }

            // 2. Artist Image (Middle Z-index = 1f)
            imageContent(
                Modifier
                    .fillMaxSize(0.85f)
                    .align(Alignment.Center)
                    .offset(y = 24.dp)
                    .zIndex(1f)
            )

            // 3. Top text (Line 1) - zIndex = 2f if 1-line (above image), 0f if 2-lines (behind image)
            AnimatedVisibility(
                visible = isTransitionCompleted,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(durationMillis = 350)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(if (isSingleLine) 2f else 0f)
            ) {
                if (!isSingleLine) {
                    AutoResizedText(
                        text = line1Text,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = line1MaxFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        minFontSize = line1MinFontSize,
                        maxFontSize = line1MaxFontSize,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .offset(y = (-16).dp)
                    )
                } else {
                    // Overlapping Artist Name on top of image (1-liner mode, edge-to-edge)
                    AutoResizedText(
                        text = line1Text,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = line1SingleLineMaxFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        minFontSize = line1MinFontSize,
                        maxFontSize = line1SingleLineMaxFontSize,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .offset(y = (-16).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistImageSearchDialog(
    initialQuery: String,
    candidateImages: List<DeezerArtist>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelectImage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            onSearch(initialQuery)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Artist Image",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search artist on Deezer...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { onSearch(searchQuery) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Results Area
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (candidateImages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Type an artist name to search" else "No images found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(candidateImages, key = { it.id }) { candidate ->
                            val candidateImageUrl = candidate.pictureXl
                                ?: candidate.pictureBig
                                ?: candidate.pictureMedium
                                ?: candidate.picture

                            if (!candidateImageUrl.isNullOrEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectImage(candidateImageUrl) }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = candidateImageUrl,
                                            contentDescription = candidate.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
