package io.github.vinaygaykar.mp1.ui.album

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import android.view.HapticFeedbackConstants
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.artist.CollaboratorArtistCard
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkItem
import io.github.vinaygaykar.mp1.ui.components.FlyingArtworkOverlay
import io.github.vinaygaykar.mp1.ui.library.MultiSelectionBottomSheet
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.library.tabs.CreatePlaylistDialog
import io.github.vinaygaykar.mp1.ui.library.tabs.PlaylistPickerBottomSheet
import io.github.vinaygaykar.mp1.ui.nowplaying.NowPlayingScreen
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import io.github.vinaygaykar.mp1.utils.splitArtistsByDelimiters
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.ceil

@AndroidEntryPoint
class AlbumDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALBUM_ID = "extra_album_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                AlbumDetailScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    val primaryArtist by viewModel.primaryArtist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val featuredArtists by viewModel.featuredArtists.collectAsState()
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

    val context = LocalContext.current
    val view = LocalView.current
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

    MaterialTheme(colorScheme = currentColorScheme) {
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
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        } else {
            Color.Transparent
        }

        val cardColor = Color.Transparent

        val currentPlayingMediaId = remember(queueState.items, queueState.currentIndex) {
            if (queueState.currentIndex in queueState.items.indices) {
                queueState.items[queueState.currentIndex].mediaId
            } else null
        }

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
        var isMiniPlayerCollapsedOnScroll by remember { mutableStateOf(false) }

        LaunchedEffect(playbackState.trackTitle) {
            isMiniPlayerCollapsedOnScroll = false
        }

        LaunchedEffect(playbackState.isPlaying) {
            if (isMiniPlayerCollapsedOnScroll) {
                isMiniPlayerCollapsedOnScroll = false
            }
        }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y
                    if (deltaY < -8f) {
                        isMiniPlayerCollapsedOnScroll = true
                    } else if (deltaY > 8f) {
                        isMiniPlayerCollapsedOnScroll = false
                    }
                    return Offset.Zero
                }
            }
        }

        val flyingArtworks = remember { mutableStateListOf<FlyingArtworkItem>() }
        var rootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }
        var rootSize by remember { mutableStateOf(IntSize.Zero) }

        val density = LocalDensity.current

        val handleAddToQueue: (Song, Offset, IntSize) -> Unit = { song, offset, size ->
            viewModel.addToQueue(song)
            if (isPlayerActive) {
                flyingArtworks.add(
                    FlyingArtworkItem(
                        artworkUri = song.artworkUri ?: album?.artworkUri,
                        startOffset = offset,
                        startSize = size
                    )
                )
            }
        }

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
                                            text = album?.name ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        if (artistName != null && hasArtist) {
                                            Text(
                                                text = artistName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Album Name in Hero Text
                    item {
                        AutoResizedText(
                            text = album?.name ?: "Loading Album...",
                            style = MaterialTheme.typography.displaySmall,
                            maxLines = 5,
                            minFontSize = 20.sp,
                            maxFontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    // 2. Album Art (Square shape, no rounded corners, edge-to-edge minus screen padding)
                    item {
                        val artworkUri = album?.artworkUri
                        if (artworkUri != null) {
                            val request = remember(artworkUri) {
                                ImageRequest.Builder(context)
                                    .data(artworkUri)
                                    .size(800, 800)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = album?.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // 3. Album Artist (or primary artist)
                    if (artistName != null && hasArtist) {
                        item {
                            var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords ->
                                        itemCoordinates = coords
                                    }
                                    .clickable {
                                        val currentPrimaryArtist = primaryArtist
                                        if (currentPrimaryArtist != null) {
                                            val offset = itemCoordinates?.positionInRoot() ?: Offset.Zero
                                            val size = itemCoordinates?.size ?: IntSize.Zero
                                            val intent = Intent(context, ArtistDetailActivity::class.java).apply {
                                                putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, currentPrimaryArtist.id)
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
                                    }
                                    .basicMarquee()
                            )
                        }
                    }

                    // 4. Release Year
                    item {
                        val yearText = remember(album?.year) { album?.year?.takeIf { it > 0 }?.toString() }
                        if (yearText != null) {
                            Text(
                                text = yearText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Row before song list: Track Count & Duration (left) and Shuffle Button (right)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val trackCount = songs.size
                            val tracksText = if (trackCount == 1) "1 track" else "$trackCount tracks"
                            val totalDurationMs = remember(songs) { songs.sumOf { it.durationMs } }
                            val durationText = remember(totalDurationMs) { formatAlbumTotalTime(totalDurationMs) }

                            Column {
                                Text(
                                    text = tracksText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (durationText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = { viewModel.playAlbum(shuffle = true) },
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

                    // 5. List of Songs (Grouped by Disc if multi-disc, else flat list)
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
                                        .padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }
                            itemsIndexed(discSongs, key = { _, song -> song.id }) { index, song ->
                                val isCurrentSong = (currentPlayingMediaId == song.id) ||
                                        (currentPlayingMediaId.isNullOrEmpty() && playbackState.trackTitle == song.title)
                                SongCardItem(
                                    song = song,
                                    trackIndex = index + 1,
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
                    } else {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            val isCurrentSong = (currentPlayingMediaId == song.id) ||
                                    (currentPlayingMediaId.isNullOrEmpty() && playbackState.trackTitle == song.title)
                            SongCardItem(
                                song = song,
                                trackIndex = index + 1,
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

                    // Featured Artists Horizontal List (after song list)
                    if (featuredArtists.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
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
                                        .padding(horizontal = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(featuredArtists, key = { it.id }) { artist ->
                                        CollaboratorArtistCard(
                                            artist = artist,
                                            playingArtist = playbackState.artist,
                                            playingAlbum = playbackState.album,
                                            isPlaying = playbackState.isPlaying,
                                            onClick = { offset, size ->
                                                val intent = Intent(context, ArtistDetailActivity::class.java).apply {
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Now Playing Screen on top
            NowPlayingScreen(
                modifier = Modifier.fillMaxSize(),
                playbackState = playbackState,
                progressState = progressState,
                queueState = queueState,
                lyricsState = lyricsState,
                playbackControls = playerViewModel,
                queueControls = playerViewModel,
                lyricsControls = playerViewModel,
                onProgressUpdate = { expansionProgress.floatValue = it },
                visualizerState = visualizerState,
                bulgeTrigger = miniPlayerBulgeTrigger,
                isCollapsedOnScroll = isMiniPlayerCollapsedOnScroll
            ) { miniPlayerTargetOffset = it }

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
        }
    }
}

@Composable
fun SongCardItem(
    song: Song,
    trackIndex: Int,
    cardColor: Color = Color.Transparent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

