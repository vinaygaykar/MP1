package io.github.vinaygaykar.mp1.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data structure holding target song and initial circular artwork bounds for hero transition.
 */
data class SongOverlayData(
    val song: Song,
    val startOffset: Offset,
    val startSize: IntSize,
    val hasOfflineLyrics: Boolean = false
)

/**
 * Reusable wrapper container that applies background blur to parent layout contents
 * and hosts the hero circular artwork animation overlay when a song's kebab option is triggered.
 */
@Composable
fun SongHeroOverlayContainer(
    modifier: Modifier = Modifier,
    overlayData: SongOverlayData? = null,
    onDismiss: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onShowFile: ((Song) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val blurRadius by animateDpAsState(
        targetValue = if (overlayData != null) 24.dp else 0.dp,
        label = "songHeroOverlayBlur"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(if (overlayData != null) 100f else 0f)
    ) {
        // Parent Screen Content (blurred when overlayData != null)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            content()
        }

        // Single-Window Hero Artwork Overlay
        if (overlayData != null) {
            SongDetailOverlay(
                song = overlayData.song,
                startOffset = overlayData.startOffset,
                startSize = overlayData.startSize,
                hasOfflineLyrics = overlayData.hasOfflineLyrics,
                onDismiss = onDismiss,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onShowFile = onShowFile
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SongDetailOverlay(
    song: Song,
    startOffset: Offset,
    startSize: IntSize,
    hasOfflineLyrics: Boolean = false,
    onDismiss: () -> Unit,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onShowFile: ((Song) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val activity = context as? Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val widthClass = windowSizeClass?.widthSizeClass
    val heightClass = windowSizeClass?.heightSizeClass

    val isWideWindow = if (widthClass != null) {
        widthClass != WindowWidthSizeClass.Compact
    } else {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val isCompactHeight = if (heightClass != null) {
        heightClass == WindowHeightSizeClass.Compact
    } else {
        configuration.screenHeightDp < 500
    }

    var isClosing by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val animProgress = remember { Animatable(0f) }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "songDetailOverlayExpand"
    )

    // Fetch album artwork embedded from song file
    var embeddedArtworkBytes by remember(song.uri) { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(song.uri) {
        if (song.uri != Uri.EMPTY) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, song.uri)
                    val pic = retriever.embeddedPicture
                    retriever.release()
                    embeddedArtworkBytes = pic
                } catch (_: Exception) {
                    embeddedArtworkBytes = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val dismissWithAnimation: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            coroutineScope.launch {
                animProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
                onDismiss()
            }
        }
    }

    BackHandler {
        if (isExpanded) {
            isExpanded = false
        } else {
            dismissWithAnimation()
        }
    }

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val p = animProgress.value

    val normalTargetSizeDp = when {
        isWideWindow -> 160.dp
        isCompactHeight -> 120.dp
        else -> 180.dp
    }
    val expandedTargetSizeDp = when {
        isWideWindow -> 220.dp
        isCompactHeight -> 150.dp
        else -> 260.dp
    }
    val targetSizeDp = normalTargetSizeDp + (expandedTargetSizeDp - normalTargetSizeDp) * expandProgress
    val targetSizePx = with(density) { targetSizeDp.toPx() }

    val targetX = if (isWideWindow) {
        val collapsedTargetX = (screenWidthPx / 4f) - (targetSizePx / 2f)
        val expandedTargetX = (screenWidthPx - targetSizePx) / 2f
        collapsedTargetX + (expandedTargetX - collapsedTargetX) * expandProgress
    } else {
        (screenWidthPx - targetSizePx) / 2f
    }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val targetY = if (isWideWindow) {
        statusBarTopPx + with(density) { (16.dp + 36.dp + 12.dp).toPx() }
    } else {
        with(density) { (56.dp + 48.dp + 20.dp).toPx() }
    }

    val artworkShadowElevation = 18.dp * expandProgress

    val startSizePx = if (startSize.width > 0) startSize.width.toFloat() else with(density) { 48.dp.toPx() }
    val initialX = if (startOffset != Offset.Zero) startOffset.x else (screenWidthPx - startSizePx) / 2f
    val initialY = if (startOffset != Offset.Zero) startOffset.y else (screenHeightPx - startSizePx) / 2f

    val currentSizePx = startSizePx + (targetSizePx - startSizePx) * p
    val currentSizeDp = with(density) { currentSizePx.toDp() }

    val artworkAnimTranslationX = (initialX - targetX) * (1f - p)
    val artworkAnimTranslationY = (initialY - targetY) * (1f - p)

    // Extracted File Type (MP3, FLAC, WAV, etc.)
    val fileExtension = remember(song.fileName, song.uri) {
        var ext = song.fileName.substringAfterLast('.', "").uppercase()
        if (ext.isEmpty() || ext.length > 5) {
            val path = song.uri.path ?: ""
            ext = path.substringAfterLast('.', "").uppercase()
        }
        if (ext.isEmpty() || ext.length > 5) "AUDIO" else ext
    }

    // Formatted Audio Track Duration
    val formattedDuration = remember(song.durationMs) { formatTrackDuration(song.durationMs) }

    val buttonsAlpha = (1f - expandProgress * 1.5f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        // Transparent backdrop layer (triggers dismiss on click)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissWithAnimation
                )
        )

        when {
            isWideWindow -> {
                SongDetailLandscapeContent(
                    song = song,
                    embeddedArtworkBytes = embeddedArtworkBytes,
                    hasOfflineLyrics = hasOfflineLyrics,
                    formattedDuration = formattedDuration,
                    fileExtension = fileExtension,
                    currentSizeDp = currentSizeDp,
                    artworkAnimTranslationX = artworkAnimTranslationX,
                    artworkAnimTranslationY = artworkAnimTranslationY,
                    artworkShadowElevation = artworkShadowElevation,
                    p = p,
                    isExpanded = isExpanded,
                    expandProgress = expandProgress,
                    buttonsAlpha = buttonsAlpha,
                    dismissWithAnimation = dismissWithAnimation,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile
                )
            }
            isCompactHeight -> {
                SongDetailCompactContent(
                    song = song,
                    embeddedArtworkBytes = embeddedArtworkBytes,
                    hasOfflineLyrics = hasOfflineLyrics,
                    formattedDuration = formattedDuration,
                    fileExtension = fileExtension,
                    currentSizeDp = currentSizeDp,
                    artworkAnimTranslationX = artworkAnimTranslationX,
                    artworkAnimTranslationY = artworkAnimTranslationY,
                    artworkShadowElevation = artworkShadowElevation,
                    p = p,
                    isExpanded = isExpanded,
                    expandProgress = expandProgress,
                    buttonsAlpha = buttonsAlpha,
                    dismissWithAnimation = dismissWithAnimation,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile
                )
            }
            else -> {
                SongDetailPortraitContent(
                    song = song,
                    embeddedArtworkBytes = embeddedArtworkBytes,
                    hasOfflineLyrics = hasOfflineLyrics,
                    formattedDuration = formattedDuration,
                    fileExtension = fileExtension,
                    currentSizeDp = currentSizeDp,
                    artworkAnimTranslationX = artworkAnimTranslationX,
                    artworkAnimTranslationY = artworkAnimTranslationY,
                    artworkShadowElevation = artworkShadowElevation,
                    p = p,
                    isExpanded = isExpanded,
                    expandProgress = expandProgress,
                    buttonsAlpha = buttonsAlpha,
                    dismissWithAnimation = dismissWithAnimation,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile
                )
            }
        }

        // Floating Circular Expand / Collapse Layout Button (FilledTonalIconButton with no shadow)
        FilledTonalIconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                contentDescription = if (isExpanded) "Collapse layout" else "Expand layout",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SongDetailPortraitContent(
    song: Song,
    embeddedArtworkBytes: ByteArray?,
    hasOfflineLyrics: Boolean,
    formattedDuration: String,
    fileExtension: String,
    currentSizeDp: Dp,
    artworkAnimTranslationX: Float,
    artworkAnimTranslationY: Float,
    artworkShadowElevation: Dp,
    p: Float,
    isExpanded: Boolean,
    expandProgress: Float,
    buttonsAlpha: Float,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val scrollModifier = if (!isExpanded) Modifier.verticalScroll(scrollState) else Modifier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .graphicsLayer {
                alpha = p
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub container 1: Song title, album artwork, texts and other metadata
        val firstContainerScale = 1f + (0.05f * expandProgress)
        val firstContainerModifier = if (isExpanded) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier.fillMaxWidth()
        }

        Column(
            modifier = firstContainerModifier
                .graphicsLayer {
                    scaleX = firstContainerScale
                    scaleY = firstContainerScale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing when expanded to move track title down from top edge
            if (isExpanded) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // 1. Song Title (Constant headlineSmall typography)
            Text(
                text = song.title.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(20.dp))

            // 2. Animated Circular Artwork
            SongArtworkBox(
                song = song,
                embeddedArtworkBytes = embeddedArtworkBytes,
                currentSizeDp = currentSizeDp,
                artworkAnimTranslationX = artworkAnimTranslationX,
                artworkAnimTranslationY = artworkAnimTranslationY,
                artworkShadowElevation = artworkShadowElevation
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(20.dp))

            // 3. Album Name
            SongAlbumTitle(
                album = song.album,
                albumId = song.albumId,
                onDismiss = dismissWithAnimation
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(12.dp))

            // 4. List of Artists (Hidden when expanded)
            if (!isExpanded) {
                val displayArtists = remember(song.artistsDisplayString, song.artist) {
                    song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
                }
                Text(
                    text = displayArtists,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Metadata Info Row (Track Duration, File Format, Offline Lyrics chips left-aligned)
            if (!isExpanded) {
                SongMetadataChips(
                    formattedDuration = formattedDuration,
                    fileExtension = fileExtension,
                    hasOfflineLyrics = hasOfflineLyrics
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sub container 2: Action Buttons
        if (buttonsAlpha > 0.01f) {
            SongActionButtons(
                song = song,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onShowFile = onShowFile,
                onDismiss = dismissWithAnimation,
                buttonHeight = 52.dp,
                spacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        alpha = buttonsAlpha
                        translationY = with(density) { (30.dp * expandProgress).toPx() }
                    }
            )
        }
    }
}

@Composable
private fun SongDetailLandscapeContent(
    song: Song,
    embeddedArtworkBytes: ByteArray?,
    hasOfflineLyrics: Boolean,
    formattedDuration: String,
    fileExtension: String,
    currentSizeDp: Dp,
    artworkAnimTranslationX: Float,
    artworkAnimTranslationY: Float,
    artworkShadowElevation: Dp,
    p: Float,
    isExpanded: Boolean,
    expandProgress: Float,
    buttonsAlpha: Float,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?
) {
    val density = LocalDensity.current
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                alpha = p
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pane: 1st sub column of data (album artwork and metadata)
        val firstContainerScale = 1f + (0.05f * expandProgress)
        val leftPaneModifier = Modifier
            .fillMaxHeight()
            .weight(1f)

        Column(
            modifier = leftPaneModifier
                .verticalScroll(leftScrollState)
                .graphicsLayer {
                    scaleX = firstContainerScale
                    scaleY = firstContainerScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status bar top spacer so content initially starts below status bar but scrolls under it
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(16.dp))

            // Top spacing when expanded
            if (isExpanded) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // 1. Song Title
            Text(
                text = song.title.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(12.dp))

            // 2. Animated Circular Artwork
            SongArtworkBox(
                song = song,
                embeddedArtworkBytes = embeddedArtworkBytes,
                currentSizeDp = currentSizeDp,
                artworkAnimTranslationX = artworkAnimTranslationX,
                artworkAnimTranslationY = artworkAnimTranslationY,
                artworkShadowElevation = artworkShadowElevation
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(12.dp))

            // 3. Album Name
            SongAlbumTitle(
                album = song.album,
                albumId = song.albumId,
                onDismiss = dismissWithAnimation
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(8.dp))

            // 4. List of Artists (Hidden when expanded)
            if (!isExpanded) {
                val displayArtists = remember(song.artistsDisplayString, song.artist) {
                    song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
                }
                Text(
                    text = displayArtists,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom spacing when expanded
            if (isExpanded) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Navigation bar bottom spacer so content scrolls above navigation bar
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Right Pane: 2nd sub column of metadata & buttons
        if (buttonsAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .verticalScroll(rightScrollState)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = buttonsAlpha
                        translationX = with(density) { (30.dp * expandProgress).toPx() }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status bar top spacer
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Info Row (Track Duration, File Format, Offline Lyrics chips)
                if (!isExpanded) {
                    SongMetadataChips(
                        formattedDuration = formattedDuration,
                        fileExtension = fileExtension,
                        hasOfflineLyrics = hasOfflineLyrics
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                SongActionButtons(
                    song = song,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile,
                    onDismiss = dismissWithAnimation,
                    buttonHeight = 48.dp,
                    spacing = 8.dp
                )

                // Navigation bar bottom spacer
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SongDetailCompactContent(
    song: Song,
    embeddedArtworkBytes: ByteArray?,
    hasOfflineLyrics: Boolean,
    formattedDuration: String,
    fileExtension: String,
    currentSizeDp: Dp,
    artworkAnimTranslationX: Float,
    artworkAnimTranslationY: Float,
    artworkShadowElevation: Dp,
    p: Float,
    isExpanded: Boolean,
    expandProgress: Float,
    buttonsAlpha: Float,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = p }
    ) {
        // Background Artwork with Dark Overlay (visible only when collapsed)
        val bgAlpha = (1f - expandProgress).coerceIn(0f, 1f)
        if (bgAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bgAlpha }
            ) {
                val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                val artworkModel = embeddedArtworkBytes ?: song.artworkUri

                AsyncImage(
                    model = artworkModel,
                    contentDescription = null,
                    error = musicPlaceholder,
                    placeholder = musicPlaceholder,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark overlay so text is clearly visible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }
        }

        // Main Foreground Content (Single Column - always scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Track Title
            Text(
                text = song.title.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isExpanded) MaterialTheme.colorScheme.onSurface else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (isExpanded) {
                // When expanded: show track title, circular album artwork, and collapse button
                Spacer(modifier = Modifier.height(24.dp))

                // 2. Circular Album Artwork
                SongArtworkBox(
                    song = song,
                    embeddedArtworkBytes = embeddedArtworkBytes,
                    currentSizeDp = currentSizeDp,
                    artworkAnimTranslationX = artworkAnimTranslationX,
                    artworkAnimTranslationY = artworkAnimTranslationY,
                    artworkShadowElevation = artworkShadowElevation
                )

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // When collapsed: show space, album name, artists, metadata chips, action buttons
                Spacer(modifier = Modifier.height(48.dp))

                // 2. Album Name
                SongAlbumTitle(
                    album = song.album,
                    albumId = song.albumId,
                    onDismiss = dismissWithAnimation,
                    textColor = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. List of Artists
                val displayArtists = remember(song.artistsDisplayString, song.artist) {
                    song.artistsDisplayString.ifBlank { song.artist.ifBlank { "Unknown Artist" } }
                }
                Text(
                    text = displayArtists,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Metadata Info Chips
                SongMetadataChips(
                    formattedDuration = formattedDuration,
                    fileExtension = fileExtension,
                    hasOfflineLyrics = hasOfflineLyrics
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Action Buttons
                if (buttonsAlpha > 0.01f) {
                    SongActionButtons(
                        song = song,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onShowFile = onShowFile,
                        onDismiss = dismissWithAnimation,
                        buttonHeight = 44.dp,
                        spacing = 8.dp,
                        playNextColor = Color(0xFF90CAF9),
                        addToQueueColor = Color(0xFFA5D6A7),
                        addToPlaylistColor = Color(0xFFCE93D8),
                        showFileColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                alpha = buttonsAlpha
                                translationY = with(density) { (20.dp * expandProgress).toPx() }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongArtworkBox(
    song: Song,
    embeddedArtworkBytes: ByteArray?,
    currentSizeDp: Dp,
    artworkAnimTranslationX: Float,
    artworkAnimTranslationY: Float,
    artworkShadowElevation: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = artworkAnimTranslationX
                translationY = artworkAnimTranslationY
            }
            .size(currentSizeDp)
            .shadow(
                elevation = artworkShadowElevation,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
        val artworkModel = embeddedArtworkBytes ?: song.artworkUri
        AsyncImage(
            model = artworkModel,
            contentDescription = song.title,
            error = musicPlaceholder,
            placeholder = musicPlaceholder,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun SongAlbumTitle(
    album: String,
    albumId: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val displayAlbum = remember(album) { album.ifBlank { "Unknown Album" } }
    Surface(
        onClick = {
            val albumIdentifier = albumId?.ifEmpty { null } ?: album
            if (albumIdentifier.isNotBlank()) {
                val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                    putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumIdentifier)
                }
                context.startActivity(intent)
                onDismiss()
            }
        },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Text(
            text = displayAlbum,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SongMetadataChips(
    formattedDuration: String,
    fileExtension: String,
    hasOfflineLyrics: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        // Audio Track Time
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Duration",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // File Type (MP3, FLAC, WAV, etc.)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = "File Type",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = fileExtension,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Offline Lyrics Indicator
        if (hasOfflineLyrics) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Offline Lyrics",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SongActionButtons(
    song: Song,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 52.dp,
    spacing: Dp = 12.dp,
    playNextColor: Color = MaterialTheme.colorScheme.primary,
    addToQueueColor: Color = MaterialTheme.colorScheme.secondary,
    addToPlaylistColor: Color = MaterialTheme.colorScheme.tertiary,
    showFileColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Play Next Button
        TextButton(
            onClick = {
                onPlayNext?.invoke(song)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonHeight / 2),
            colors = ButtonDefaults.textButtonColors(
                contentColor = playNextColor
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Play next",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Add to Queue Button
        TextButton(
            onClick = {
                onAddToQueue?.invoke(song)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonHeight / 2),
            colors = ButtonDefaults.textButtonColors(
                contentColor = addToQueueColor
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Add to Playlist Button
        TextButton(
            onClick = {
                onAddToPlaylist?.invoke(song)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonHeight / 2),
            colors = ButtonDefaults.textButtonColors(
                contentColor = addToPlaylistColor
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Show File Button
        TextButton(
            onClick = {
                if (onShowFile != null) {
                    onShowFile(song)
                } else {
                    openSongParentFolder(context, song)
                }
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            shape = RoundedCornerShape(buttonHeight / 2),
            colors = ButtonDefaults.textButtonColors(
                contentColor = showFileColor
            )
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Show file",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Show file",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatTrackDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun openSongParentFolder(context: Context, song: Song) {
    try {
        val uri = song.uri
        if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            val parentFolder = file.parentFile ?: file
            val folderUri = Uri.fromFile(parentFolder)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Folder"))
        } else {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Show File"))
        }
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = song.uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// =============================================================================
// PREVIEWS (Portrait, Landscape, Compact Mode)
// =============================================================================

private val samplePreviewSong = Song(
    id = "1",
    uri = Uri.EMPTY,
    title = "Lorem Ipsum Dolor",
    artist = "Consectetur Adipiscing",
    album = "Sed Do Eiusmod",
    albumId = "sample_album_1",
    durationMs = 234000L,
    trackNumber = 1,
    discNumber = 1,
    year = 2024,
    artworkUri = null,
    fileName = "lorem_ipsum.flac",
    lastModified = 0L,
    artistsDisplayString = "Lorem Ipsum, Dolor Sit Amet"
)

@Preview(
    name = "Portrait Light",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Compact / Split",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Compact / Split",
    group = "Song Hero Overlay",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun SongHeroOverlayContainerPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SongHeroOverlayContainer(
                overlayData = SongOverlayData(
                    song = samplePreviewSong,
                    startOffset = Offset(100f, 400f),
                    startSize = IntSize(144, 144),
                    hasOfflineLyrics = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
