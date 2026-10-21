package io.github.vinaygaykar.mp1.ui.artist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.vinaygaykar.mp1.data.network.models.DeezerArtist
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.domain.models.Artist
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.album.SongCardItem
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.nowplaying.NowPlayingScreen
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.library.MultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.ui.theme.isArtistMatchingPlayingTrack
import io.github.vinaygaykar.mp1.ui.theme.rememberMorphingArtistShape
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArtistDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ARTIST_ID = "extra_artist_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                ArtistDetailScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
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
    val progressState by playerViewModel.progressState.collectAsState()
    val queueState by playerViewModel.queueState.collectAsState()
    val lyricsState by playerViewModel.lyricsState.collectAsState()
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

    val candidateImages by viewModel.candidateImages.collectAsState()
    val isSearchingImages by viewModel.isSearchingImages.collectAsState()

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
    val view = LocalView.current
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
    val expansionProgress = remember { mutableFloatStateOf(0f) }

    val lazyListState = rememberLazyListState()

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 1 ||
                    (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset > 40)
        }
    }

    val topBarContainerColor = if (showTopBarTitle) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color.Transparent
    }

    val cardColor = Color.Transparent

    val currentPlayingMediaId = remember(queueState.items, queueState.currentIndex) {
        if (queueState.currentIndex in queueState.items.indices) {
            queueState.items[queueState.currentIndex].mediaId
        } else null
    }

    var miniPlayerBulgeTrigger by remember { mutableLongStateOf(0L) }
    var miniPlayerTargetOffset by remember { mutableStateOf<Offset?>(null) }
    val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
    var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

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

    MaterialTheme(colorScheme = currentColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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

        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomPadding = if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 16.dp + navBarBottom

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
            containerColor = MaterialTheme.colorScheme.background,
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
                                enter = fadeIn(tween(200)) + slideInVertically(animationSpec = tween(200)) { it / 2 },
                                exit = fadeOut(tween(150)) + slideOutVertically(animationSpec = tween(150)) { it / 2 }
                            ) {
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(
                                        text = artist?.name ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            FilledTonalIconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(start = 4.dp)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artist Header Image & Name Redesign with AutoResizedText
                item {
                    val currentArtist = artist
                    val displayedArtistName = currentArtist?.name ?: "Loading Artist..."
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
                        artistId = currentArtist?.id ?: "",
                        artistName = currentArtist?.name ?: "",
                        isMorphing = isMorphing
                    )

                    ArtistHeaderWithAutoResizedName(
                        artistName = displayedArtistName,
                        imageContent = { imageModifier ->
                            Box(
                                modifier = imageModifier
                                    .clip(artistShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        if (!artistImageUrl.isNullOrEmpty()) {
                                            previewImageUrl = artistImageUrl
                                        } else {
                                            showSearchImageDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!artistImageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = artistImageUrl,
                                        contentDescription = displayedArtistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = displayedArtistName,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    )
                }

                // Song Count
                item {
                    val count = songs.size
                    Text(
                        text = "$count ${if (count == 1) "song" else "songs"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Albums Horizontal List
                if (sortedAlbums.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sortedAlbums, key = { it.id }) { album ->
                                    ArtistAlbumCard(
                                        album = album,
                                        onClick = { offset, size ->
                                            val intent = Intent(context, AlbumDetailActivity::class.java).apply {
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
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Featured On Horizontal List
                if (sortedFeaturedAlbums.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Features",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sortedFeaturedAlbums, key = { it.id }) { album ->
                                    ArtistAlbumCard(
                                        album = album,
                                        onClick = { offset, size ->
                                            val intent = Intent(context, AlbumDetailActivity::class.java).apply {
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
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Discography (Songs grouped by Album)
                if (discographyGroups.isNotEmpty()) {
                    item(key = "discography_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Discography",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalIconButton(
                                onClick = { viewModel.playArtist(shuffle = true) },
                                enabled = songs.isNotEmpty(),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle"
                                )
                            }
                        }
                    }

                    discographyGroups.forEach { (albumName, matchingAlbum, albumSongs) ->
                        item(key = "discography_album_header_$albumName") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val albumArtwork = matchingAlbum?.artworkUri ?: albumSongs.firstOrNull { it.artworkUri != null }?.artworkUri
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
                                    val trackCountStr = "${albumSongs.size} ${if (albumSongs.size == 1) "song" else "tracks"}"
                                    val subtitle = listOfNotNull(yearStr, trackCountStr).joinToString(" • ")
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        itemsIndexed(albumSongs, key = { _, song -> song.id }) { index, song ->
                            val isCurrentSong = (currentPlayingMediaId == song.id) ||
                                    (currentPlayingMediaId.isNullOrEmpty() && playbackState.trackTitle == song.title)
                            SongCardItem(
                                song = song,
                                trackIndex = song.trackNumber ?: (index + 1),
                                isCurrentSong = isCurrentSong,
                                cardColor = cardColor,
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

                // Collaborators Horizontal List (after songs list)
                if (collaborators.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (collaborators.size == 1) "Featured Artist" else "Featured Artists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(collaborators, key = { it.id }) { collaborator ->
                                    CollaboratorArtistCard(
                                        artist = collaborator,
                                        playingArtist = playbackState.artist,
                                        playingAlbum = playbackState.album,
                                        isPlaying = playbackState.isPlaying,
                                        onClick = { offset, size ->
                                            val intent = Intent(context, ArtistDetailActivity::class.java).apply {
                                                putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, collaborator.id)
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
                                        }
                                    )
                                }
                            }
                        }
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
            bulgeTrigger = miniPlayerBulgeTrigger,
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
            PlaylistPickerBottomSheet(
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
                onSearch = { query -> viewModel.searchArtistImages(query) },
                onSelectImage = { selectedUrl ->
                    viewModel.selectArtistImage(selectedUrl)
                    showSearchImageDialog = false
                },
                onDismiss = { showSearchImageDialog = false }
            )
        }
    }
}
}

@Composable
fun ArtistAlbumCard(
    album: Album,
    onClick: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable {
                val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                val size = itemCoordinates?.size ?: IntSize.Zero
                onClick(offset, size)
            },
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onGloballyPositioned { coords ->
                    itemCoordinates = coords
                },
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
    onClick: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier,
    playingArtist: String = "",
    playingAlbum: String = "",
    isPlaying: Boolean = false
) {
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
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
            .onGloballyPositioned { coords ->
                itemCoordinates = coords
            }
            .clickable {
                val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                val size = itemCoordinates?.size ?: IntSize.Zero
                onClick(offset, size)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // Line 1 above the artist image (drawn BEFORE image in Box so image overlays if overlapping)
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
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp)
                        .offset(y = (-12).dp)
                )
            }

            // Artist Image (kept at same dimension: 0.85f width, 1:1 aspect ratio)
            imageContent(
                Modifier
                    .fillMaxSize(0.85f)
                    .align(Alignment.Center)
            )

            if (isSingleLine) {
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
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp)
                        .offset(y = (-12).dp)
                )
            } else if (line2Text.isNotBlank()) {
                // Line 2 below the image OVERLAPPING it (edge-to-edge)
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
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
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
