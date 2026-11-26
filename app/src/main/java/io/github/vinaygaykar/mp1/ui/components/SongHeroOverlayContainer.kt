package io.github.vinaygaykar.mp1.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import io.github.vinaygaykar.mp1.domain.models.Song
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
 * Reusable wrapper container that applies Haze backdrop blur to parent layout contents
 * and hosts the hero circular artwork animation overlay when a song's kebab option is triggered.
 */
@Composable
fun SongHeroOverlayContainer(
    modifier: Modifier = Modifier,
    overlayData: SongOverlayData? = null,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onShowFile: ((Song) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val effectiveHazeState = hazeState ?: remember { HazeState() }

    val blurRadius by animateDpAsState(
        targetValue = if (overlayData != null) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "songHeroOverlayBlur"
    )

    val backdropAlpha by animateFloatAsState(
        targetValue = if (overlayData != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "songHeroOverlayBackdropAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(if (overlayData != null) 100f else 0f)
            .then(
                if (overlayData != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                event.changes.forEach { change ->
                                    if (change.positionChange() != Offset.Zero) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // Parent Screen Content (Haze blur source)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = effectiveHazeState)
        ) {
            content()
        }

        // Haze backdrop blur overlay when overlayData != null
        if (backdropAlpha > 0f) {
            val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropAlpha }
                    .hazeEffect(
                        state = effectiveHazeState,
                        style = HazeStyle(
                            backgroundColor = containerColor,
                            blurRadius = blurRadius,
                            tint = HazeTint(containerColor.copy(alpha = 0.35f))
                        )
                    )
            )
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
                onShowFile = onShowFile,
                onAlbumClick = onAlbumClick
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
    onShowFile: ((Song) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null
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

    // World-fixed light direction (gravity driven) for the CD shine
    val cdLight = rememberCdLight(active = isExpanded)

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
        // Transparent backdrop layer (hijacks touch events; empty space clicks do NOT dismiss overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Do not dismiss on clicking empty space */ }
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
                    cdLight = cdLight,
                    dismissWithAnimation = dismissWithAnimation,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile,
                    onAlbumClick = onAlbumClick
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
                    cdLight = cdLight,
                    dismissWithAnimation = dismissWithAnimation,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowFile = onShowFile,
                    onAlbumClick = onAlbumClick
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
                    cdLight = cdLight,
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
    cdLight: CdLight? = null,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?,
    onAlbumClick: ((String) -> Unit)? = null
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
                artworkShadowElevation = artworkShadowElevation,
                shineAlpha = expandProgress,
                cdLight = cdLight
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(20.dp))

            // 3. Album Name
            SongAlbumTitle(
                album = song.album,
                albumId = song.albumId,
                onDismiss = dismissWithAnimation,
                onAlbumClick = onAlbumClick
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
    cdLight: CdLight? = null,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?,
    onAlbumClick: ((String) -> Unit)? = null
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
                artworkShadowElevation = artworkShadowElevation,
                shineAlpha = expandProgress,
                cdLight = cdLight
            )

            Spacer(modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(12.dp))

            // 3. Album Name
            SongAlbumTitle(
                album = song.album,
                albumId = song.albumId,
                onDismiss = dismissWithAnimation,
                onAlbumClick = onAlbumClick
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
    cdLight: CdLight? = null,
    dismissWithAnimation: () -> Unit,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    onAddToPlaylist: ((Song) -> Unit)?,
    onShowFile: ((Song) -> Unit)?,
    onAlbumClick: ((String) -> Unit)? = null
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
                    artworkShadowElevation = artworkShadowElevation,
                    shineAlpha = expandProgress,
                    cdLight = cdLight
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
                    onAlbumClick = onAlbumClick,
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

private object CdShine {
    /** Master knob 0..1. ~0.25 subtle, ~0.55 balanced, ~0.9 bold. */
    const val INTENSITY = 0.75f

    // Tilt
    const val MAX_TILT = 1.5f
    const val IDLE_STRENGTH = 0.15f          // shine level when phone is flat

    // Spectral bands
    const val BAND_REPEATS = 2
    const val BAND_FILL_FRACTION = 0.45f     // portion of each segment that has color, rest is dark
    const val SPECTRAL_ALPHA = 0.75f
    const val BAND_ANGLE_FACTOR = -0.6f      // bands counter-rotate vs lobes so colors slide through them
    const val BAND_TRAVEL_DEG_PER_TILT = 70f

    // Highlight lobes (fractions of 360°)
    const val LOBE_HALF_WIDTH = 0.09f
    const val HIGHLIGHT_HALF_WIDTH = 0.05f
    const val HIGHLIGHT_ALPHA = 0.55f

    // Center fade (fractions of radius)
    const val CENTER_FADE_START = 0.10f
    const val CENTER_FADE_END = 0.45f

    // Grooves
    const val GROOVE_SPACING_DP = 2.5f
    const val GROOVE_STROKE_DP = 0.6f
    const val GROOVE_ALPHA = 0.18f

    // Rim
    const val RIM_STROKE_DP = 1.5f
    const val RIM_ALPHA = 0.7f

    val SPECTRUM = listOf(
        Color(0xFF8F00FF), // violet
        Color(0xFF2979FF), // blue
        Color(0xFF00E676), // green
        Color(0xFFFFEA00), // yellow
        Color(0xFFFF1744)  // red
    )
}

private fun buildSpectralStops(): Array<Pair<Float, Color>> = buildList {
    val segment = 1f / CdShine.BAND_REPEATS
    val bandSpan = segment * CdShine.BAND_FILL_FRACTION
    val step = bandSpan / (CdShine.SPECTRUM.size + 1)
    repeat(CdShine.BAND_REPEATS) { k ->
        val start = k * segment
        add(start to CdShine.SPECTRUM.first().copy(alpha = 0f))
        CdShine.SPECTRUM.forEachIndexed { i, c -> add(start + step * (i + 1) to c) }
        add(start + bandSpan to CdShine.SPECTRUM.last().copy(alpha = 0f))
    }
    add(1f to CdShine.SPECTRUM.first().copy(alpha = 0f))
}.toTypedArray()

private fun buildLobeStops(halfWidth: Float, color: Color): Array<Pair<Float, Color>> {
    val clear = color.copy(alpha = 0f)
    return arrayOf(
        0f to color,
        halfWidth to clear,
        0.5f - halfWidth to clear,
        0.5f to color,
        0.5f + halfWidth to clear,
        1f - halfWidth to clear,
        1f to color
    )
}

private val SpectralStops = buildSpectralStops()
private val MaskLobeStops = buildLobeStops(CdShine.LOBE_HALF_WIDTH, Color.Black)
private val HighlightLobeStops = buildLobeStops(CdShine.HIGHLIGHT_HALF_WIDTH, Color.White)

// =============================================================================
// CD SHINE (API 33+ AGSL diffraction shader + gravity-driven world-fixed light)
// =============================================================================

private object CdShineConfig {
    /** Light direction, degrees away from straight up (0 = overhead, 90 = horizontal, toward the viewer). */
    const val LIGHT_ANGLE_DEG = 15f

    /** Light size. Lobe half-width (sine) = 0.08 + 1.2 * softness; also blurs the colour bands. */
    const val LIGHT_SOFTNESS = 0.05f

    /** Overall brightness of the shine (0..~1.5). */
    const val INTENSITY = 0.4f

    /** Rainbow strength relative to the white zero-order. */
    const val COLOR_GAIN = 3.0f

    /** Groove pitch in nm. Real CD = 1600. Larger = narrower rainbow, more overlapping orders. */
    const val GROOVE_PITCH_NM = 1600f

    /** Rainbow brightness outside the lobes (0 = none, 1 = no lobe shaping). */
    const val SCATTER_FLOOR = 0.04f

    /** Shine fades in between these radii (fraction of disc radius). Hides the centre. */
    const val FADE_START = 0.10f
    const val FADE_END = 0.45f

    /** Concentric grooves (drawn only inside the lobes, like the old canvas version). */
    const val GROOVE_SPACING_DP = 2.5f
    const val GROOVE_STROKE_DP = 0.6f
    const val GROOVE_ALPHA = 0.25f

    /** Gravity low-pass factor (0..1, higher = snappier). */
    const val SENSOR_SMOOTHING = 0.18f
}

/**
 * Smoothed gravity direction ("up") in the device frame, plus the world-fixed light derived from it.
 * Device frame: x = right of screen, y = top of screen, z = out of the screen toward the viewer.
 * The light is LIGHT_ANGLE_DEG from vertical, tilted toward the viewer. Gravity only -> no compass dependency.
 */
@Stable
private class CdLight {
    var upX by mutableFloatStateOf(0f); private set
    var upY by mutableFloatStateOf(0f); private set
    var upZ by mutableFloatStateOf(1f); private set

    fun update(gx: Float, gy: Float, gz: Float) {
        val n = sqrt(gx * gx + gy * gy + gz * gz)
        if (n < 1e-3f) return
        val a = CdShineConfig.SENSOR_SMOOTHING
        var x = upX + (gx / n - upX) * a
        var y = upY + (gy / n - upY) * a
        var z = upZ + (gz / n - upZ) * a
        val m = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-4f)
        x /= m; y /= m; z /= m
        upX = x; upY = y; upZ = z
    }

    fun reset() {
        upX = 0f; upY = 0f; upZ = 1f
    }

    /** Unit light direction in the device frame, written into [out] (size 3). */
    fun lightDirection(out: FloatArray) {
        val ux = upX
        val uy = upY
        val uz = upZ

        // Horizontal "toward the viewer": device +z with the vertical component removed.
        var hzx = -uz * ux
        var hzy = -uz * uy
        var hzz = 1f - uz * uz
        val lenZ = sqrt(hzx * hzx + hzy * hzy + hzz * hzz)
        // Fallback when the phone is flat (+z is vertical): use the top of the screen instead.
        var hyx = -uy * ux
        var hyy = 1f - uy * uy
        var hyz = -uy * uz
        val lenY = sqrt(hyx * hyx + hyy * hyy + hyz * hyz)

        val k = smoothstep(0.10f, 0.35f, lenZ)
        if (lenZ > 1e-4f) { hzx /= lenZ; hzy /= lenZ; hzz /= lenZ } else { hzx = 0f; hzy = 0f; hzz = 0f }
        if (lenY > 1e-4f) { hyx /= lenY; hyy /= lenY; hyz /= lenY } else { hyx = 0f; hyy = 0f; hyz = 0f }

        var hx = hzx * k + hyx * (1f - k)
        var hy = hzy * k + hyy * (1f - k)
        var hz = hzz * k + hyz * (1f - k)
        val hl = sqrt(hx * hx + hy * hy + hz * hz)
        if (hl > 1e-4f) { hx /= hl; hy /= hl; hz /= hl }

        val rad = Math.toRadians(CdShineConfig.LIGHT_ANGLE_DEG.toDouble())
        val ca = cos(rad).toFloat()
        val sa = sin(rad).toFloat()
        var lx = ca * ux + sa * hx
        var ly = ca * uy + sa * hy
        var lz = ca * uz + sa * hz
        val ll = sqrt(lx * lx + ly * ly + lz * lz).coerceAtLeast(1e-4f)
        lx /= ll; ly /= ll; lz /= ll
        out[0] = lx; out[1] = ly; out[2] = lz
    }

    /** Approximation of the old tilt scalars, for the pre-API-33 gradient fallback. */
    val legacyTiltX: Float get() = (-upX * 1.96f).coerceIn(-1.5f, 1.5f)
    val legacyTiltY: Float get() = (upY * 1.96f).coerceIn(-1.5f, 1.5f)

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

/** Listens to gravity (TYPE_GRAVITY, falling back to the accelerometer) while [active]. */
@Composable
private fun rememberCdLight(active: Boolean): CdLight {
    val context = LocalContext.current
    val light = remember { CdLight() }

    DisposableEffect(active) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (!active || sm == null || sensor == null) {
            light.reset()
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val v = event?.values ?: return
                    // Both sensors read +9.8 along the direction that points up when the phone is at rest.
                    light.update(v[0], v[1], v[2])
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    return light
}

/**
 * Draws the diffraction shine over the content (Screen blend). No-op below API 33 or when [light] is null.
 * Apply AFTER `.clip(CircleShape)`. [shineAlpha] fades it in/out.
 */
@Composable
private fun Modifier.cdShine(light: CdLight?, shineAlpha: Float): Modifier =
    if (light != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        cdShineAgsl(light, shineAlpha)
    } else {
        this
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.cdShineAgsl(light: CdLight, shineAlpha: Float): Modifier {
    val shader = remember { RuntimeShader(CD_SHINE_AGSL) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val dir = remember { FloatArray(3) }

    return drawWithContent {
        drawContent()
        if (shineAlpha > 0.001f) {
            light.lightDirection(dir) // state read happens in draw -> redraw only, no recomposition
            shader.setFloatUniform("uRes", size.width, size.height)
            shader.setFloatUniform("uLight", dir[0], dir[1], dir[2])
            shader.setFloatUniform("uSoft", CdShineConfig.LIGHT_SOFTNESS)
            shader.setFloatUniform("uGain", CdShineConfig.INTENSITY * shineAlpha)
            shader.setFloatUniform("uColorGain", CdShineConfig.COLOR_GAIN)
            shader.setFloatUniform("uPitch", CdShineConfig.GROOVE_PITCH_NM)
            shader.setFloatUniform("uFloor", CdShineConfig.SCATTER_FLOOR)
            shader.setFloatUniform("uFade", CdShineConfig.FADE_START, CdShineConfig.FADE_END)
            shader.setFloatUniform(
                "uGroove",
                CdShineConfig.GROOVE_SPACING_DP.dp.toPx(),
                CdShineConfig.GROOVE_STROKE_DP.dp.toPx(),
                CdShineConfig.GROOVE_ALPHA
            )
            drawRect(brush = brush, blendMode = BlendMode.Screen)
        }
    }
}

/**
 * Reflection-grating model of a CD (radial groove direction, pitch uPitch nm, orders 1..3).
 * With H = L + V (V = +z, orthographic) at polar angle phi on the disc:
 *   s = H . radial  -> which wavelengths diffract toward the eye: lambda = pitch * |s| / m
 *   t = H . tangent -> must be ~0 (conical condition) -> gives the straight-edged lobes
 * Lobe width is angular: exp(-(t/|H_inplane|)^2 / sa^2), sa = 0.08 + 1.2 * softness.
 */
private const val CD_SHINE_AGSL = """
uniform float2 uRes;
uniform float3 uLight;
uniform float uSoft;
uniform float uGain;
uniform float uColorGain;
uniform float uPitch;
uniform float uFloor;
uniform float2 uFade;
uniform float3 uGroove;   // x = spacing px, y = stroke px, z = alpha

float3 spectrum(float l) {
    float3 c;
    if (l < 440.0) c = float3(-(l - 440.0) / 60.0, 0.0, 1.0);
    else if (l < 490.0) c = float3(0.0, (l - 440.0) / 50.0, 1.0);
    else if (l < 510.0) c = float3(0.0, 1.0, -(l - 510.0) / 20.0);
    else if (l < 580.0) c = float3((l - 510.0) / 70.0, 1.0, 0.0);
    else if (l < 645.0) c = float3(1.0, -(l - 645.0) / 65.0, 0.0);
    else c = float3(1.0, 0.0, 0.0);
    float f = 1.0;
    if (l < 420.0) f = 0.3 + 0.7 * (l - 380.0) / 40.0;
    else if (l > 700.0) f = 0.3 + 0.7 * (750.0 - l) / 50.0;
    float vis = step(380.0, l) * step(l, 750.0);
    return c * f * vis;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * uRes) / (0.5 * min(uRes.x, uRes.y));
    float r = length(uv);
    if (r > 1.0) return half4(0.0);

    float2 pd = float2(uv.x, -uv.y);              // screen y points down, device y points up
    float2 rho = pd / max(r, 1e-4);               // groove normal (radial)
    float2 tau = float2(-rho.y, rho.x);           // along the groove

    float3 L = normalize(uLight);
    float3 H = L + float3(0.0, 0.0, 1.0);         // light + view (view = out of screen)

    float lf = smoothstep(0.0, 0.1, L.z);         // light must hit the data face
    float s = dot(H.xy, rho);
    float t = dot(H.xy, tau);
    float hp = length(H.xy);
    float cb = t / max(hp, 1e-3);
    float sa = 0.08 + uSoft * 1.2;
    float lobe = mix(1.0, exp(-cb * cb / (sa * sa)), smoothstep(0.05, 0.3, hp));

    float delta = uSoft * 0.3;
    float3 df = float3(0.0);
    for (int i = 0; i < 12; i++) {
        float ss = abs(s + (float(i) / 11.0 - 0.5) * 2.0 * delta);
        for (int m = 1; m < 4; m++) {
            float fm = float(m);
            float w = 0.2;
            if (m == 1) w = 1.0;
            else if (m == 2) w = 0.45;
            df += spectrum(uPitch * ss / fm) * w;
        }
    }
    df /= 12.0;
    df *= uFloor + (1.0 - uFloor) * lobe;

    float z = s / (uSoft * 2.0 + 0.05);
    float w0 = exp(-z * z) * lobe;                // white zero-order

    float mask = smoothstep(uFade.x, uFade.y, r);

    // Concentric grooves: thin anti-aliased rings, visible only inside the lobes
    float rPx = r * 0.5 * min(uRes.x, uRes.y);
    float dRing = abs(fract(rPx / uGroove.x) - 0.5) * uGroove.x;
    float ring = 1.0 - smoothstep(uGroove.y * 0.5, uGroove.y * 0.5 + 1.0, dRing);

    float3 col = lf * (df * uColorGain + w0 * float3(0.9) + ring * uGroove.z * lobe * float3(1.0)) * mask;
    col = 1.0 - exp(-col * 1.4);
    return half4(half3(col * uGain), 1.0);
}
"""

@Composable
private fun SongArtworkBox(
    song: Song,
    embeddedArtworkBytes: ByteArray?,
    currentSizeDp: Dp,
    artworkAnimTranslationX: Float,
    artworkAnimTranslationY: Float,
    artworkShadowElevation: Dp,
    modifier: Modifier = Modifier,
    shineAlpha: Float = 0f,
    cdLight: CdLight? = null
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .cdShine(cdLight, shineAlpha),
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

        if (shineAlpha > 0.001f && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .graphicsLayer {
                        alpha = shineAlpha
                        compositingStrategy = CompositingStrategy.Offscreen // needed for DstIn masking
                    }
            ) {
                val c = center
                val radius = size.minDimension / 2f
                val shineTiltX = cdLight?.legacyTiltX ?: 0f
                val shineTiltY = cdLight?.legacyTiltY ?: 0f
                val tilt = kotlin.math.hypot(shineTiltX, shineTiltY)
                val strength = (tilt / CdShine.MAX_TILT).coerceIn(0f, 1f)
                val energy = (CdShine.IDLE_STRENGTH + (1f - CdShine.IDLE_STRENGTH) * strength) *
                    CdShine.INTENSITY
                val angleDeg = Math.toDegrees(
                    kotlin.math.atan2(shineTiltY, shineTiltX).toDouble()
                ).toFloat()
                val bandRotation = angleDeg * CdShine.BAND_ANGLE_FACTOR +
                    tilt * CdShine.BAND_TRAVEL_DEG_PER_TILT

                // 1. Spectral bands
                rotate(bandRotation, c) {
                    drawRect(
                        brush = Brush.sweepGradient(*SpectralStops, center = c),
                        alpha = CdShine.SPECTRAL_ALPHA * energy
                    )
                }

                // 2. Concentric grooves
                val spacing = CdShine.GROOVE_SPACING_DP.dp.toPx()
                val grooveStroke = Stroke(width = CdShine.GROOVE_STROKE_DP.dp.toPx())
                var r = spacing
                while (r < radius) {
                    drawCircle(
                        color = Color.White,
                        radius = r,
                        center = c,
                        alpha = CdShine.GROOVE_ALPHA * energy,
                        style = grooveStroke
                    )
                    r += spacing
                }

                // 3. Highlight lobes
                rotate(angleDeg, c) {
                    drawRect(
                        brush = Brush.sweepGradient(*HighlightLobeStops, center = c),
                        alpha = CdShine.HIGHLIGHT_ALPHA * energy,
                        blendMode = BlendMode.Plus
                    )
                }

                // 4. Bow-tie mask
                rotate(angleDeg, c) {
                    drawRect(
                        brush = Brush.sweepGradient(*MaskLobeStops, center = c),
                        blendMode = BlendMode.DstIn
                    )
                }

                // 5. Center fade
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0f),
                            CdShine.CENTER_FADE_START to Color.Black.copy(alpha = 0f),
                            CdShine.CENTER_FADE_END to Color.Black,
                            1f to Color.Black
                        ),
                        center = c,
                        radius = radius
                    ),
                    blendMode = BlendMode.DstIn
                )

                // 6. Rim highlight
                val rimStroke = CdShine.RIM_STROKE_DP.dp.toPx()
                rotate(angleDeg, c) {
                    drawCircle(
                        brush = Brush.sweepGradient(*HighlightLobeStops, center = c),
                        radius = radius - rimStroke / 2f,
                        center = c,
                        alpha = CdShine.RIM_ALPHA * energy,
                        style = Stroke(width = rimStroke),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
    }
}

@Composable
private fun SongAlbumTitle(
    album: String,
    albumId: String?,
    onDismiss: () -> Unit,
    onAlbumClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val displayAlbum = remember(album) { album.ifBlank { "Unknown Album" } }
    Surface(
        onClick = {
            val albumIdentifier = albumId?.ifEmpty { null } ?: album
            if (albumIdentifier.isNotBlank()) {
                onAlbumClick?.invoke(albumIdentifier)
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
