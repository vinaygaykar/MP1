package io.github.vinaygaykar.mp1.ui.library.v2

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaygaykar.mp1.R
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.library.tabs.AlbumsTab
import io.github.vinaygaykar.mp1.ui.library.tabs.ArtistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistGrid
import io.github.vinaygaykar.mp1.ui.library.tabs.SongList
import io.github.vinaygaykar.mp1.ui.playlist.PlaylistDetailActivity

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
    dragOffset: Float,
    isPlayerActive: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenCategoryMenu: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenScanLocations: () -> Unit,
    onActionBarExpandedChange: (Boolean) -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onImportPlaylistLauncher: () -> Unit,
    onAddFlyingArtwork: (FlyingArtworkItem) -> Unit,
    modifier: Modifier = Modifier,
    categoryHeaderTitleModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    val collapsedFraction = scrollBehavior.state.collapsedFraction

    LaunchedEffect(collapsedFraction) {
        onActionBarExpandedChange(collapsedFraction < 0.1f)
    }

    var noticeBannerHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.isScanning) {
        if (!uiState.isScanning) {
            noticeBannerHeightPx = 0f
        }
    }

    val isFloatingVisible by remember(scrollBehavior) {
        derivedStateOf {
            scrollBehavior.state.collapsedFraction > 0.9f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dragOffset
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // Scan Notice Banner
            AnimatedVisibility(
                visible = uiState.isScanning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        noticeBannerHeightPx = if (uiState.isScanning) coordinates.size.height.toFloat() else 0f
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = categoryHeaderTitleModifier
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

            // Sticky Selection Row if Active
            AnimatedVisibility(
                visible = (libraryState.lastOpenedCategory == LibraryCategory.SONGS && libraryState.isSelectionMode) ||
                        (libraryState.lastOpenedCategory == LibraryCategory.PLAYLISTS && libraryState.isPlaylistSelectionMode),
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
                } else if (libraryState.lastOpenedCategory == LibraryCategory.SONGS) {
                    SelectionActionRow(
                        selectedCount = libraryState.selectedSongIds.size,
                        onSelectAll = { libraryState.selectAllSongs(uiState.songs) },
                        onDeselect = { libraryState.clearSongSelection() },
                        onOptionsClick = { libraryState.showMultiSelectionSheet = true }
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
                            onPlaylistClick = { playlist ->
                                if (libraryState.isPlaylistSelectionMode) {
                                    libraryState.togglePlaylistSelection(playlist)
                                } else {
                                    val intent = Intent(
                                        context,
                                        PlaylistDetailActivity::class.java
                                    ).apply {
                                        putExtra(
                                            PlaylistDetailActivity.EXTRA_PLAYLIST_ID,
                                            playlist.id
                                        )
                                        putExtra(
                                            PlaylistDetailActivity.EXTRA_PLAYLIST_NAME,
                                            playlist.name
                                        )
                                    }
                                    context.startActivity(intent)
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
                            selectedSongIds = libraryState.selectedSongIds,
                            isSelectionMode = libraryState.isSelectionMode,
                            onSwipeRight = { song, offset, size ->
                                onAddToQueue(song)
                                if (isPlayerActive) {
                                    onAddFlyingArtwork(
                                        FlyingArtworkItem(
                                            artworkUri = song.artworkUri,
                                            startOffset = offset,
                                            startSize = size
                                        )
                                    )
                                }
                            },
                            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                            headerHeight = 104.dp
                        )
                    }

                    LibraryCategory.ALBUMS -> {
                        AlbumsTab(
                            albums = uiState.albums,
                            useCleanAlbumGrid = uiState.useCleanAlbumGrid,
                            contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
                        )
                    }

                    LibraryCategory.ARTISTS -> {
                        ArtistGrid(
                            artists = uiState.artists,
                            onArtistClick = { artist, offset, size ->
                                val intent =
                                    Intent(context, ArtistDetailActivity::class.java).apply {
                                        putExtra(
                                            ArtistDetailActivity.EXTRA_ARTIST_ID,
                                            artist.id
                                        )
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

                    else -> {}
                }
            }
        }

        // Floating Search Button visible when action bar is hidden
        if (isFloatingVisible) {
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
    }
}
