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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    onAlbumClick: ((String) -> Unit)? = null,
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

    // Gyroscope / Motion tracking for metallic sheen reflection
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    var rawTiltX by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }

    val shineTiltX by animateFloatAsState(
        targetValue = if (isExpanded) rawTiltX else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "shineTiltX"
    )
    val shineTiltY by animateFloatAsState(
        targetValue = if (isExpanded) rawTiltY else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "shineTiltY"
    )

    DisposableEffect(isExpanded, sensorManager) {
        if (!isExpanded || sensorManager == null) {
            rawTiltX = 0f
            rawTiltY = 0f
            onDispose { }
        } else {
            val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val listener = object : SensorEventListener {
                // Last in-plane direction (world-up as seen on screen), held while the phone is flat
                var dirX = 0f
                var dirY = 1f

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    val gx = event.values[0]
                    val gy = event.values[1]
                    val gz = event.values[2]
                    val g = kotlin.math.sqrt(gx * gx + gy * gy + gz * gz)
                    if (g < 1e-3f) return
                    val px = gx / g
                    val py = gy / g
                    val blend = kotlin.math.hypot(px, py).coerceIn(0f, 1f) // 0 flat, 1 upright
                    if (blend > CdShine.FLAT_DEADZONE) {
                        dirX = px / blend
                        dirY = py / blend
                    }
                    rawTiltX = dirX * blend
                    rawTiltY = dirY * blend
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (gravitySensor != null) {
                sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

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
                    shineTiltX = shineTiltX,
                    shineTiltY = shineTiltY,
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
                    shineTiltX = shineTiltX,
                    shineTiltY = shineTiltY,
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
                    shineTiltX = shineTiltX,
                    shineTiltY = shineTiltY,
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
    shineTiltX: Float = 0f,
    shineTiltY: Float = 0f,
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
                shineTiltX = shineTiltX,
                shineTiltY = shineTiltY
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
    shineTiltX: Float = 0f,
    shineTiltY: Float = 0f,
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
                shineTiltX = shineTiltX,
                shineTiltY = shineTiltY
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
    shineTiltX: Float = 0f,
    shineTiltY: Float = 0f,
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
                    shineTiltX = shineTiltX,
                    shineTiltY = shineTiltY
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
    const val INTENSITY = 0.55f

    // Tilt
    const val MAX_TILT = 1f                  // tilt is now the flat→upright blend, 0..1
    const val IDLE_STRENGTH = 0.7f           // flat should already show clear conical wedges
    const val FLAT_DEADZONE = 0.08f          // below this, hold the last in-plane angle
    const val LIGHT_AZIMUTH_DEG = 45f        // light direction clockwise from screen-up (45 = top right)

    // Spectral bands
    const val BAND_REPEATS = 2
    const val BAND_FILL_FRACTION = 0.45f     // portion of each segment that has color, rest is dark
    const val SPECTRAL_ALPHA = 0.75f
    const val BAND_ANGLE_FACTOR = -0.6f      // bands counter-rotate vs lobes so colors slide through them
    const val BAND_TRAVEL_DEG_PER_TILT = 70f
    const val PHASE_PER_TILT = 0.5f              // how fast colors slide through the lobe per unit of tilt
    const val WEDGE_WIDTH_DEG = 45f          // one full spectrum from the lobe axis to this angle (mirrored on both sides)
    const val VIRTUAL_CENTER_DIST = 2.2f     // radii from the CD center toward the light, outside the disc
    const val GLARE_WIDTH = 0.12f            // white band thickness, in radii
    const val RAINBOW_SPAN = 0.9f            // radial thickness of the one spectrum pass, in radii
    const val GLARE_SOFTNESS = 0.4f          // glare falloff, in spectrum units
    const val RADIAL_WHITEN = 0.55f          // how far lobe A fades toward white at upright
    const val METAL_SHEEN_ALPHA = 0.25f
    val METAL_COLOR = Color(0xFF9AA4AD)

    // Lobe shapes at upright (A = light side, B = opposite)
    const val LOBE_A_UPRIGHT_HALF_WIDTH = 0.20f
    const val LOBE_B_UPRIGHT_HALF_WIDTH = 0.05f
    const val HIGHLIGHT_A_UPRIGHT_HALF_WIDTH = 0.10f
    const val LOBE_B_UPRIGHT_DIM = 0.6f      // lobe B alpha reduction at upright

    // Highlight lobes (fractions of 360°)
    const val LOBE_HALF_WIDTH = 0.14f        // must be >= WEDGE_WIDTH_DEG / 360 so the spectrum is not clipped
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

private fun buildLobeStops(
    halfA: Float, halfB: Float, colorA: Color, colorB: Color
): Array<Pair<Float, Color>> = arrayOf(
    0f to colorA,
    halfA to colorA.copy(alpha = 0f),
    0.5f - halfB to colorB.copy(alpha = 0f),
    0.5f to colorB,
    0.5f + halfB to colorB.copy(alpha = 0f),
    1f - halfA to colorA.copy(alpha = 0f),
    1f to colorA
)

private val SpectralStops = buildSpectralStops()

/** Builds the AGSL source; colors and band layout come from [CdShine] so there is one source of truth. */
private fun buildSpectralAgsl(): String {
    val n = CdShine.SPECTRUM.size
    fun rgb(c: Color) = "half3(${c.red}, ${c.green}, ${c.blue})"
    val body = buildString {
        append("if (pos < 1.0) return half4(${rgb(CdShine.SPECTRUM.first())}, half(pos));\n")
        for (k in 1 until n) {
            append("if (pos < ${k + 1}.0) return half4(mix(${rgb(CdShine.SPECTRUM[k - 1])}, ${rgb(CdShine.SPECTRUM[k])}, half(pos - ${k}.0)), 1.0);\n")
        }
        append("return half4(${rgb(CdShine.SPECTRUM.last())}, half(1.0 - (pos - ${n}.0)));\n")
    }
    return """
        uniform float2 uCenter;
        uniform float uRadius;
        uniform float uLightAngle;
        uniform float uPhase;
        uniform float uBlend;
        uniform float2 uVirtualCenter;
        uniform float uWedgeWidth;
        uniform float uGlareEdge;
        uniform float uRainbowSpan;

        half4 spectrum(float pos) {
            $body
        }

        // One spectrum pass for t in 0..1, nothing outside (no repeats)
        half4 once(float t) {
            if (t < 0.0 || t > 1.0) return half4(0.0);
            half4 c = spectrum(t * ${n + 1}.0);
            return half4(c.rgb * c.a, c.a);
        }

        half4 main(float2 p) {
            float2 d = p - uCenter;
            float r = length(d) / uRadius;
            float a = atan(d.y, d.x) - uLightAngle;
            float ca = cos(a);
            // Flat: angular distance to the nearest lobe axis (mirrored across the axis and across both lobes)
            float da = abs(asin(clamp(sin(a), -1.0, 1.0)));
            half4 conical = once(da / uWedgeWidth);
            // Upright: arcs concentric around a virtual center outside the disc, white glare first, then one spectrum pass
            float dv = length(p - uVirtualCenter) / uRadius;
            float t = (dv - uGlareEdge) / uRainbowSpan - uPhase;
            half4 radial = once(t);
            half g = half(1.0 - smoothstep(0.0, ${CdShine.GLARE_SOFTNESS}, abs(t + 0.2)));
            radial = half4(radial.rgb + g, max(radial.a, g));
            // w: 0 flat or on lobe B side, up to 1 on lobe A at upright
            float w = uBlend * smoothstep(0.0, 1.0, ca);
            half4 c = mix(conical, radial, half(w));
            c.rgb = mix(c.rgb, half3(c.a), half(w * ${CdShine.RADIAL_WHITEN}));
            return c;
        }
    """.trimIndent()
}

/** Returns null below API 33. Typed as Any to keep RuntimeShader out of signatures on older devices. */
private fun createSpectralShader(): Any? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) createSpectralShaderApi33() else null

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createSpectralShaderApi33(): Any = RuntimeShader(buildSpectralAgsl())

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawSpectralShader(
    shader: RuntimeShader,
    center: Offset,
    radius: Float,
    lightAngleDeg: Float,
    tilt: Float,
    blend: Float,
    alpha: Float
) {
    shader.setFloatUniform("uCenter", center.x, center.y)
    shader.setFloatUniform("uRadius", radius)
    shader.setFloatUniform("uLightAngle", Math.toRadians(lightAngleDeg.toDouble()).toFloat())
    shader.setFloatUniform("uPhase", tilt * CdShine.PHASE_PER_TILT)
    val theta = Math.toRadians(lightAngleDeg.toDouble())
    val dist = radius * CdShine.VIRTUAL_CENTER_DIST
    shader.setFloatUniform(
        "uVirtualCenter",
        center.x + kotlin.math.cos(theta).toFloat() * dist,
        center.y + kotlin.math.sin(theta).toFloat() * dist
    )
    shader.setFloatUniform("uWedgeWidth", Math.toRadians(CdShine.WEDGE_WIDTH_DEG.toDouble()).toFloat())
    shader.setFloatUniform("uGlareEdge", CdShine.VIRTUAL_CENTER_DIST - 1f + CdShine.GLARE_WIDTH)
    shader.setFloatUniform("uRainbowSpan", CdShine.RAINBOW_SPAN)
    shader.setFloatUniform("uBlend", blend)
    drawRect(brush = ShaderBrush(shader), alpha = alpha)
}

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
    shineTiltX: Float = 0f,
    shineTiltY: Float = 0f
) {
    val spectralShader = remember { createSpectralShader() }

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

        if (shineAlpha > 0.001f) {
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
                val tilt = kotlin.math.hypot(shineTiltX, shineTiltY)
                val strength = (tilt / CdShine.MAX_TILT).coerceIn(0f, 1f)
                val energy = (CdShine.IDLE_STRENGTH + (1f - CdShine.IDLE_STRENGTH) * strength) *
                    CdShine.INTENSITY
                val blend = tilt.coerceIn(0f, 1f)
                // Canvas y points down, so negate tiltY to get screen angle of world-up
                val angleDeg = Math.toDegrees(
                    kotlin.math.atan2(-shineTiltY, shineTiltX).toDouble()
                ).toFloat() + CdShine.LIGHT_AZIMUTH_DEG
                val bandRotation = angleDeg * CdShine.BAND_ANGLE_FACTOR +
                    tilt * CdShine.BAND_TRAVEL_DEG_PER_TILT

                val dimB = 1f - CdShine.LOBE_B_UPRIGHT_DIM * blend
                val maskStops = buildLobeStops(
                    androidx.compose.ui.util.lerp(CdShine.LOBE_HALF_WIDTH, CdShine.LOBE_A_UPRIGHT_HALF_WIDTH, blend),
                    androidx.compose.ui.util.lerp(CdShine.LOBE_HALF_WIDTH, CdShine.LOBE_B_UPRIGHT_HALF_WIDTH, blend),
                    Color.Black, Color.Black.copy(alpha = dimB)
                )
                val highlightStops = buildLobeStops(
                    androidx.compose.ui.util.lerp(CdShine.HIGHLIGHT_HALF_WIDTH, CdShine.HIGHLIGHT_A_UPRIGHT_HALF_WIDTH, blend),
                    CdShine.HIGHLIGHT_HALF_WIDTH,
                    Color.White, Color.White.copy(alpha = dimB)
                )

                // 1. Spectral diffraction bands (narrow, with dark gaps)
                if (spectralShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    drawSpectralShader(
                        shader = spectralShader as RuntimeShader,
                        center = c,
                        radius = radius,
                        lightAngleDeg = angleDeg,
                        tilt = tilt,
                        blend = blend,
                        alpha = CdShine.SPECTRAL_ALPHA * energy
                    )
                } else {
                    // Fallback (API < 33): no radial hue shift
                    rotate(bandRotation, c) {
                        drawRect(
                            brush = Brush.sweepGradient(*SpectralStops, center = c),
                            alpha = CdShine.SPECTRAL_ALPHA * energy
                        )
                    }
                }

                // 1b. Faint metallic base sheen
                drawRect(color = CdShine.METAL_COLOR, alpha = CdShine.METAL_SHEEN_ALPHA * energy)

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

                // 3. Specular highlight lobes (additive)
                rotate(angleDeg, c) {
                    drawRect(
                        brush = Brush.sweepGradient(*highlightStops, center = c),
                        alpha = CdShine.HIGHLIGHT_ALPHA * energy,
                        blendMode = BlendMode.Plus
                    )
                }

                // 4. Mask everything to the two opposing lobes (bow-tie)
                rotate(angleDeg, c) {
                    drawRect(
                        brush = Brush.sweepGradient(*maskStops, center = c),
                        blendMode = BlendMode.DstIn
                    )
                }

                // 5. Fade out near the center to avoid the pinch point
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

                // 6. Rim highlight (drawn after fade so it stays crisp)
                val rimStroke = CdShine.RIM_STROKE_DP.dp.toPx()
                rotate(angleDeg, c) {
                    drawCircle(
                        brush = Brush.sweepGradient(*highlightStops, center = c),
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
