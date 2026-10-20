package io.github.vinaygaykar.mp1.ui.nowplaying

import android.view.HapticFeedbackConstants
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import androidx.media3.common.MediaItem
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.PlaybackWaveType
import io.github.vinaygaykar.mp1.domain.models.VisualizerState
import io.github.vinaygaykar.mp1.playback.LyricsControls
import io.github.vinaygaykar.mp1.playback.LyricsUiState
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueControls
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.components.AutoResizedText
import io.github.vinaygaykar.mp1.ui.library.DragValue
import io.github.vinaygaykar.mp1.ui.library.calculateAngle
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.AudioEnergyExtractor
import io.github.vinaygaykar.mp1.utils.AudioFrequencyExtractor
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import io.github.vinaygaykar.mp1.utils.toArtistId
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class ColorStep(
    val color: Color,
    val isReverse: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandablePlayer(
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    queueState: QueueUiState,
    lyricsState: LyricsUiState = LyricsUiState.None,
    playbackControls: PlaybackControls,
    queueControls: QueueControls,
    lyricsControls: LyricsControls,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier,
    visualizerState: VisualizerState = VisualizerState.None,
    peekHeight: Dp = 72.dp,
    bottomOffset: Dp = 0.dp,
    initialExpanded: Boolean = false,
    bulgeTrigger: Long = 0L,
    onMiniPlayerPositioned: ((Offset) -> Unit)? = null
) {
    val expandAnimProgress = remember { Animatable(1f) }
    var lastTrackTitle by remember { mutableStateOf(if (initialExpanded) playbackState.trackTitle else "") }

    LaunchedEffect(playbackState.trackTitle) {
        if (lastTrackTitle.isEmpty() && playbackState.trackTitle.isNotEmpty()) {
            expandAnimProgress.snapTo(0f)
            delay(1000L)
            expandAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        } else if (playbackState.trackTitle.isEmpty()) {
            expandAnimProgress.snapTo(0f)
        }
        lastTrackTitle = playbackState.trackTitle
    }

    if (playbackState.trackTitle.isEmpty()) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val animatableBulge = remember { Animatable(1f) }
    LaunchedEffect(bulgeTrigger) {
        if (bulgeTrigger > 0L) {
            animatableBulge.snapTo(1f)
            animatableBulge.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            )
            animatableBulge.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val currentPlaybackControls by rememberUpdatedState(playbackControls)
    val currentPlaybackState by rememberUpdatedState(playbackState)
    val currentProgressState by rememberUpdatedState(progressState)

    val lifecycleOwner = LocalLifecycleOwner.current
    var isScreenActive by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isScreenActive = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isDark = isSystemInDarkTheme()
    val playerColorScheme: ColorScheme? = remember(playbackState.seedColor, isDark) {
        playbackState.seedColor?.let { color ->
            DynamicThemeHelper.createDynamicColorScheme(color, isDark = isDark)
        }
    }

    val targetBgColor = (playerColorScheme ?: MaterialTheme.colorScheme).primaryContainer

    // Song change color spread animation state
    var bgColorPair by remember { mutableStateOf<Pair<Color, Color>?>(null) }
    var isReverseRipple by remember { mutableStateOf(false) }
    var displayedIndexState by remember { mutableIntStateOf(-1) }
    var displayedColorState by remember { mutableStateOf<Color?>(null) }
    var prevQueueState by remember { mutableStateOf<List<MediaItem>?>(null) }
    var lastAnimatedTrackKey by rememberSaveable { mutableStateOf<String?>(null) }
    val rippleProgress = remember { Animatable(0f) }

    val pendingColorQueue = remember { mutableStateListOf<ColorStep>() }

    val currentTrackKey = remember(queueState.currentIndex, playbackState.trackTitle) {
        "${queueState.currentIndex}_${playbackState.trackTitle}"
    }

    // Producer: Enqueues transitions on actual track changes
    LaunchedEffect(currentTrackKey, targetBgColor) {
        val newIndex = queueState.currentIndex
        val prevIndex = displayedIndexState
        val newQueue = queueState.items
        val prevQueue = prevQueueState

        val isSameQueue = prevQueue != null &&
                prevQueue.size == newQueue.size &&
                prevQueue.indices.all { i -> prevQueue[i].mediaId == newQueue[i].mediaId }

        var currentIsReverse = isReverseRipple
        if (newIndex != prevIndex && newIndex != -1) {
            currentIsReverse = isSameQueue && prevIndex != -1 && newIndex < prevIndex
            isReverseRipple = currentIsReverse
            displayedIndexState = newIndex
            prevQueueState = newQueue
        }

        val isInitialLoad = lastAnimatedTrackKey == null || displayedColorState == null
        val isSongChanged = lastAnimatedTrackKey != null && lastAnimatedTrackKey != currentTrackKey
        val isArtworkLoading = playbackState.artworkUri != null && playbackState.seedColor == null

        if (isInitialLoad) {
            displayedColorState = targetBgColor
            lastAnimatedTrackKey = currentTrackKey
        } else if (isSongChanged) {
            val startColor = pendingColorQueue.lastOrNull()?.color ?: displayedColorState ?: targetBgColor
            if (startColor != targetBgColor && !isArtworkLoading) {
                while (pendingColorQueue.size >= 4) {
                    pendingColorQueue.removeAt(0)
                }
                pendingColorQueue.add(ColorStep(targetBgColor, currentIsReverse))
                lastAnimatedTrackKey = currentTrackKey
            } else if (startColor == targetBgColor && !isArtworkLoading) {
                lastAnimatedTrackKey = currentTrackKey
            }
        } else {
            if (pendingColorQueue.isEmpty() && bgColorPair == null) {
                displayedColorState = targetBgColor
            }
        }
    }

    // Consumer Worker: Processes pending color transitions sequentially
    LaunchedEffect(Unit) {
        while (true) {
            if (pendingColorQueue.isEmpty()) {
                snapshotFlow { pendingColorQueue.size }.first { it > 0 }
            }
            if (pendingColorQueue.isNotEmpty()) {
                val nextStep = pendingColorQueue.removeAt(0)
                val startColor = displayedColorState ?: targetBgColor
                if (startColor != nextStep.color) {
                    bgColorPair = startColor to nextStep.color
                    isReverseRipple = nextStep.isReverse

                    val duration = if (pendingColorQueue.isNotEmpty()) 350 else 800
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))

                    displayedColorState = nextStep.color
                    bgColorPair = null
                } else {
                    displayedColorState = nextStep.color
                }
            }
        }
    }

    // Dynamic Record Visuals based on playing state
    val overlayAlpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 400),
        label = "overlay_alpha"
    )

    val scale = remember { Animatable(1f) }
    val hasPrevPage = remember(queueState.items.size, queueState.currentIndex, playbackState.repeatMode) {
        if (queueState.items.size <= 1) {
            playbackState.repeatMode == Player.REPEAT_MODE_ALL
        } else {
            if (playbackState.repeatMode == Player.REPEAT_MODE_ALL) {
                true
            } else {
                queueState.currentIndex > 0
            }
        }
    }

    val hasNextPage = remember(queueState.items.size, queueState.currentIndex, playbackState.repeatMode) {
        if (queueState.items.size <= 1) {
            playbackState.repeatMode == Player.REPEAT_MODE_ALL
        } else {
            if (playbackState.repeatMode == Player.REPEAT_MODE_ALL) {
                true
            } else {
                queueState.currentIndex in 0 until queueState.items.size - 1
            }
        }
    }

    val currentTrackPageIndex = if (hasPrevPage) 1 else 0
    val totalPageCount = 1 + (if (hasPrevPage) 1 else 0) + (if (hasNextPage) 1 else 0)

    val pagerState = rememberPagerState(
        initialPage = currentTrackPageIndex,
        pageCount = { totalPageCount }
    )

    val maxSlidePx = with(density) { 24.dp.toPx() }
    val boundaryOffsetAnimatable = remember { Animatable(0f) }

    LaunchedEffect(queueState.currentIndex, playbackState.trackTitle, hasPrevPage, hasNextPage) {
        if (pagerState.currentPage != currentTrackPageIndex) {
            pagerState.scrollToPage(currentTrackPageIndex)
        }
    }

    val targetTrackPageIndex by rememberUpdatedState(currentTrackPageIndex)

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage < targetTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            playbackControls.onPrevious()
            if (pagerState.currentPage != targetTrackPageIndex) {
                pagerState.scrollToPage(targetTrackPageIndex)
            }
        } else if (pagerState.settledPage > targetTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            playbackControls.onNext()
            if (pagerState.currentPage != targetTrackPageIndex) {
                pagerState.scrollToPage(targetTrackPageIndex)
            }
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && boundaryOffsetAnimatable.value != 0f) {
            boundaryOffsetAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val boundaryNestedScrollConnection = remember(hasPrevPage, hasNextPage, totalPageCount, currentTrackPageIndex) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = boundaryOffsetAnimatable.value
                if (currentOffset == 0f) return Offset.Zero

                val deltaX = available.x
                if ((currentOffset > 0f && deltaX < 0f) || (currentOffset < 0f && deltaX > 0f)) {
                    val newOffset = if (currentOffset > 0f) {
                        (currentOffset + deltaX).coerceAtLeast(0f)
                    } else {
                        (currentOffset + deltaX).coerceAtMost(0f)
                    }
                    val consumedX = newOffset - currentOffset
                    scope.launch {
                        boundaryOffsetAnimatable.snapTo(newOffset)
                    }
                    return Offset(consumedX, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaX = available.x
                if (deltaX == 0f) return Offset.Zero

                val isAtPrevBoundary = !hasPrevPage && deltaX > 0f && pagerState.currentPage == 0
                val isAtNextBoundary = !hasNextPage && deltaX < 0f && pagerState.currentPage == totalPageCount - 1

                if (isAtPrevBoundary || isAtNextBoundary) {
                    val currentOffset = boundaryOffsetAnimatable.value
                    val newOffset = currentOffset + deltaX
                    scope.launch {
                        boundaryOffsetAnimatable.snapTo(newOffset)
                    }
                    return Offset(deltaX, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (boundaryOffsetAnimatable.value != 0f) {
                    boundaryOffsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (boundaryOffsetAnimatable.value != 0f) {
                    boundaryOffsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    var isDraggingScrub by remember { mutableStateOf(false) }
    var isDraggingArtwork by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableLongStateOf(0L) }
    var hapticAccumulator by remember { mutableFloatStateOf(0f) }
    var invalidHapticAccumulator by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    var visualRotation by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showArtistsDialog by remember { mutableStateOf(false) }

    val scrubFactor by animateFloatAsState(
        targetValue = if (isDraggingScrub) 1f else 0f,
        animationSpec = tween(400),
        label = "scrub_factor"
    )

    val rowWidthFraction by animateFloatAsState(
        targetValue = 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "row_width_fraction"
    )

    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying) {
            var lastFrameTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
                    visualRotation += deltaMs * 0.12f
                    lastFrameTime = frameTime
                }
            }
        }
    }

    LaunchedEffect(playbackState.trackTitle, playbackState.artist) {
        visualRotation = 0f
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight
        val screenWidth = this.maxWidth

        val peekHeightPx = with(density) { peekHeight.toPx() }
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val bottomOffsetPx = with(density) { bottomOffset.toPx() }
        val floatOffsetPx = with(density) { 16.dp.toPx() }
        val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
        val totalBottomGapPx = bottomOffsetPx + floatOffsetPx + navBarBottomPx

        val anchors = remember(screenHeightPx, peekHeightPx, totalBottomGapPx) {
            DraggableAnchors {
                DragValue.COLLAPSED at screenHeightPx - peekHeightPx - totalBottomGapPx
                DragValue.EXPANDED at 0f
            }
        }

        val splineBasedDecay = rememberSplineBasedDecay<Float>()

        val anchoredDraggableState = remember(initialExpanded) {
            AnchoredDraggableState(
                initialValue = if (initialExpanded) DragValue.EXPANDED else DragValue.COLLAPSED,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                decayAnimationSpec = splineBasedDecay,
                confirmValueChange = { newValue ->
                    !((showLyrics || showQueue || showArtistsDialog) && newValue == DragValue.COLLAPSED)
                }
            )
        }

        SideEffect {
            anchoredDraggableState.updateAnchors(anchors)
        }

        val queueAnchors = remember(screenHeightPx) {
            DraggableAnchors {
                DragValue.COLLAPSED at screenHeightPx
                DragValue.EXPANDED at 0f
            }
        }

        val queueAnchoredDraggableState = remember {
            AnchoredDraggableState(
                initialValue = DragValue.COLLAPSED,
                positionalThreshold = { distance: Float -> distance * 0.4f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                decayAnimationSpec = splineBasedDecay
            )
        }

        SideEffect {
            queueAnchoredDraggableState.updateAnchors(queueAnchors)
        }

        LaunchedEffect(showQueue) {
            if (showQueue) {
                if (queueAnchoredDraggableState.targetValue != DragValue.EXPANDED) {
                    queueAnchoredDraggableState.animateTo(DragValue.EXPANDED)
                }
            } else {
                if (queueAnchoredDraggableState.targetValue != DragValue.COLLAPSED) {
                    queueAnchoredDraggableState.animateTo(DragValue.COLLAPSED)
                }
            }
        }

        LaunchedEffect(queueAnchoredDraggableState) {
            snapshotFlow { queueAnchoredDraggableState.currentValue to queueAnchoredDraggableState.targetValue }
                .collect { (current, target) ->
                    if (current == DragValue.COLLAPSED && target == DragValue.COLLAPSED) {
                        if (showQueue) showQueue = false
                    } else if (current == DragValue.EXPANDED || target == DragValue.EXPANDED) {
                        if (!showQueue) showQueue = true
                    }
                }
        }

        val queueOffset = try {
            queueAnchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            if (showQueue) 0f else screenHeightPx
        }

        val queueProgress = if (screenHeightPx > 0f) {
            ((screenHeightPx - queueOffset.coerceAtLeast(0f)) / screenHeightPx).coerceIn(0f, 1f)
        } else {
            if (showQueue) 1f else 0f
        }

        val defaultOffset =
            if (initialExpanded) 0f else (screenHeightPx - peekHeightPx - totalBottomGapPx)
        val dragOffset = try {
            anchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            defaultOffset
        }
        val totalRange = screenHeightPx - peekHeightPx - totalBottomGapPx



        BackHandler(enabled = anchoredDraggableState.currentValue != DragValue.COLLAPSED || queueProgress > 0f || showQueue) {
            if (showArtistsDialog) {
                showArtistsDialog = false
            } else if (showLyrics) {
                showLyrics = false
            } else if (queueProgress > 0f || showQueue) {
                scope.launch {
                    queueAnchoredDraggableState.animateTo(DragValue.COLLAPSED)
                }
            } else {
                scope.launch { anchoredDraggableState.animateTo(DragValue.COLLAPSED) }
            }
        }
        val progress =
            if (totalRange > 0) ((totalRange - dragOffset.coerceAtLeast(0f)) / totalRange).coerceIn(
                0f,
                1f
            ) else 0f
        val currentProgress by rememberUpdatedState(progress)

        LaunchedEffect(progress) {
            onProgressUpdate(progress)
        }

        val isPlayerVisible = progress > 0.8f && isScreenActive
        val activeSongId = remember(queueState.items, queueState.currentIndex, currentTrackKey) {
            if (queueState.currentIndex in queueState.items.indices) {
                queueState.items[queueState.currentIndex].mediaId
            } else {
                currentTrackKey
            }
        }

        LaunchedEffect(activeSongId, isPlayerVisible, playbackState.playbackWave) {
            currentPlaybackControls.onUpdateVisualizerVisibility(isPlayerVisible)
        }

        val prevItem = remember(queueState.items, queueState.currentIndex, hasPrevPage) {
            if (!hasPrevPage || queueState.items.isEmpty()) null
            else {
                val prevIndex = if (queueState.currentIndex > 0) {
                    queueState.currentIndex - 1
                } else {
                    queueState.items.size - 1
                }
                queueState.items.getOrNull(prevIndex)
            }
        }
        val prevArtworkUri = remember(prevItem) { prevItem?.mediaMetadata?.artworkUri }

        val nextItem = remember(queueState.items, queueState.currentIndex, hasNextPage) {
            if (!hasNextPage || queueState.items.isEmpty()) null
            else {
                val nextIndex = if (queueState.currentIndex in 0 until queueState.items.size - 1) {
                    queueState.currentIndex + 1
                } else {
                    0
                }
                queueState.items.getOrNull(nextIndex)
            }
        }
        val nextArtworkUri = remember(nextItem) { nextItem?.mediaMetadata?.artworkUri }

        val currentColorScheme = playerColorScheme ?: MaterialTheme.colorScheme
        val horizontalPadding = lerp(20.dp, 0.dp, progress)

        val isPreview = LocalInspectionMode.current
        val effectiveExpandProgress = if (!isPreview && lastTrackTitle.isEmpty() && playbackState.trackTitle.isNotEmpty() && expandAnimProgress.value == 1f) 0f else expandAnimProgress.value

        val rawStartPadding = lerp(screenWidth - 92.dp, 20.dp, effectiveExpandProgress)
        val rawEndPadding = 20.dp

        val startPadding = lerp(rawStartPadding, 0.dp, progress)
        val endPadding = lerp(rawEndPadding, 0.dp, progress)

        val isLandscape = screenWidth > screenHeight

        val cornerRadius = lerp(50.dp, 0.dp, progress)
        val sheetShadowElevation = lerp(8.dp, 0.dp, progress)
        val surfaceAlpha = 1f

        val targetRecordSize = if (isLandscape) {
            min(screenHeight * 0.72f, screenWidth * 0.35f)
        } else {
            screenWidth * 0.85f
        }
        val recordSize = lerp(56.dp, targetRecordSize, progress)
        val artworkSize = lerp(56.dp, min(210.dp, recordSize * 0.6f), progress)

        val targetRecordX = if (isLandscape) {
            (screenWidth * 0.42f - targetRecordSize) / 2
        } else {
            (screenWidth - targetRecordSize) / 2 - horizontalPadding
        }
        val recordX = lerp(screenWidth - recordSize - 8.dp - horizontalPadding, targetRecordX, progress)

        val targetRecordY = if (isLandscape) {
            (screenHeight - targetRecordSize) / 2
        } else {
            screenHeight * 0.44f
        }
        val recordY = lerp(8.dp, targetRecordY, progress)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lerp(peekHeight, screenHeight, progress))
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .anchoredDraggable(anchoredDraggableState, Orientation.Vertical)
        ) {
            MaterialTheme(colorScheme = currentColorScheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lerp(peekHeight, screenHeight, progress))
                        .padding(start = startPadding, end = endPadding)
                        .graphicsLayer {
                            scaleX = animatableBulge.value
                        }
                        .shadow(
                            elevation = sheetShadowElevation,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                        .drawBehind {
                            val localRecordX = recordX - startPadding
                            val center = Offset(
                                x = (localRecordX + recordSize / 2).toPx(),
                                y = (recordY + recordSize / 2).toPx()
                            )
                            val pColor =
                                (bgColorPair?.first ?: displayedColorState ?: targetBgColor).copy(alpha = surfaceAlpha)
                            val cColor =
                                (bgColorPair?.second ?: displayedColorState ?: targetBgColor).copy(alpha = surfaceAlpha)
                            val maxRadius = size.width.coerceAtLeast(size.height) * 1.5f

                            if (isReverseRipple) {
                                drawRect(cColor)
                                drawCircle(
                                    color = pColor,
                                    radius = maxRadius * (1f - rippleProgress.value),
                                    center = center
                                )
                            } else {
                                drawRect(pColor)
                                drawCircle(
                                    color = cColor,
                                    radius = maxRadius * rippleProgress.value,
                                    center = center
                                )
                            }
                        },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(cornerRadius),
                    tonalElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                            .pointerInput(progress, showQueue, showLyrics, showArtistsDialog, totalRange) {
                                if (progress > 0.8f && !showQueue && !showLyrics && !showArtistsDialog) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            val currentPlayerOffset = try {
                                                anchoredDraggableState.requireOffset()
                                            } catch (_: Exception) {
                                                0f
                                            }

                                            if (dragAmount > 0f) { // Dragging DOWN
                                                if (currentPlayerOffset < totalRange) {
                                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtMost(totalRange)
                                                    anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                                    change.consume()
                                                }
                                            } else if (dragAmount < 0f) { // Dragging UP
                                                if (currentPlayerOffset > 0f) {
                                                    val newOffset = (currentPlayerOffset + dragAmount).coerceAtLeast(0f)
                                                    anchoredDraggableState.dispatchRawDelta(newOffset - currentPlayerOffset)
                                                    change.consume()
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val currentPlayerOffset = try {
                                                anchoredDraggableState.requireOffset()
                                            } catch (_: Exception) {
                                                0f
                                            }

                                            if (currentPlayerOffset > 0f) {
                                                val target = if (currentPlayerOffset > totalRange * 0.4f) DragValue.COLLAPSED else DragValue.EXPANDED
                                                scope.launch { anchoredDraggableState.animateTo(target) }
                                            }
                                        },
                                        onDragCancel = {
                                            val currentPlayerOffset = try {
                                                anchoredDraggableState.requireOffset()
                                            } catch (_: Exception) {
                                                0f
                                            }

                                            if (currentPlayerOffset > 0f) {
                                                val target = if (currentPlayerOffset > totalRange * 0.4f) DragValue.COLLAPSED else DragValue.EXPANDED
                                                scope.launch { anchoredDraggableState.animateTo(target) }
                                            }
                                        }
                                    )
                                }
                            }
                    ) {
                        // Expanded Content
                        if (progress > 0f) {
                            val trackInfoModifier = if (isLandscape) {
                                Modifier
                                    .fillMaxHeight()
                                    .width(screenWidth * 0.58f)
                                    .offset(x = screenWidth * 0.42f)
                            } else {
                                Modifier.fillMaxSize()
                            }
                            ExpandedPlayerTrackInfo(
                                playbackState = playbackState,
                                queueState = queueState,
                                modifier = trackInfoModifier,
                                pagerState = pagerState,
                                onArtistClick = {
                                    showArtistsDialog = true
                                }
                            )
                        }

                        // MiniPlayer Content
                        if (progress < 0.2f) {
                            val miniPlayerAlpha = ((0.2f - progress) / 0.2f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(peekHeight)
                                    .alpha(miniPlayerAlpha)
                                    .clickable {
                                        scope.launch {
                                            anchoredDraggableState.animateTo(DragValue.EXPANDED)
                                        }
                                    }
                            ) {
                                MiniPlayer(
                                    playbackState = playbackState,
                                    progressState = progressState,
                                    queueState = queueState,
                                    actions = playbackControls,
                                    showRecord = false,
                                    rotation = visualRotation,
                                    textAlpha = ((effectiveExpandProgress - 0.15f) / 0.85f).coerceIn(0f, 1f),
                                    onClick = {
                                        scope.launch {
                                            anchoredDraggableState.animateTo(DragValue.EXPANDED)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Floating Overlays (Record, Lyrics, Queue, Controls)
                    Box(modifier = Modifier.fillMaxSize()) {
                        val controlsAlpha = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)

                        // Top Collapse Handle
                        if (progress > 0.8f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .alpha(controlsAlpha)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            anchoredDraggableState.animateTo(DragValue.COLLAPSED)
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(currentColorScheme.onPrimaryContainer.copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Action Buttons (Lyrics, Repeat, Queue)
                        if (progress > 0.8f) {
                            val (repeatIcon, repeatText) = when (playbackState.repeatMode) {
                                1 -> Icons.Rounded.RepeatOne to "Song"
                                2 -> Icons.Rounded.RepeatOn to "List"
                                else -> Icons.Rounded.Repeat to "Off"
                            }

                            if (isLandscape) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 24.dp, bottom = 20.dp)
                                        .alpha(controlsAlpha),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = { showLyrics = true },
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lyrics,
                                            contentDescription = "Lyrics",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Surface(
                                        onClick = { playbackControls.onRepeatToggle() },
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(48.dp),
                                        shape = CircleShape,
                                        color = if (playbackState.repeatMode != 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
                                        },
                                        contentColor = if (playbackState.repeatMode != 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = repeatIcon,
                                                contentDescription = repeatText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = repeatText,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    FilledIconButton(
                                        onClick = { showQueue = true },
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = "Playing Queue",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                FilledIconButton(
                                    onClick = { showLyrics = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 24.dp, bottom = 48.dp)
                                        .alpha(controlsAlpha)
                                        .size(48.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lyrics,
                                        contentDescription = "Lyrics",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Surface(
                                    onClick = { playbackControls.onRepeatToggle() },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 48.dp)
                                        .alpha(controlsAlpha)
                                        .width(110.dp)
                                        .height(48.dp),
                                    shape = CircleShape,
                                    color = if (playbackState.repeatMode != 0) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
                                    },
                                    contentColor = if (playbackState.repeatMode != 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = repeatIcon,
                                            contentDescription = repeatText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = repeatText,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                FilledIconButton(
                                    onClick = { showQueue = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 24.dp, bottom = 48.dp)
                                        .alpha(controlsAlpha)
                                        .size(48.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = "Playing Queue",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Timestamp display
                        if (progress > 0.8f) {
                            val displayPosition =
                                if (isDraggingScrub) draggedPosition else progressState.positionMs
                            val (currentEnergy, currentEnergyDelta) = remember(visualizerState, displayPosition) {
                                if (visualizerState is VisualizerState.Energy) {
                                    val curve = visualizerState.curve
                                    val bucketIndex = (displayPosition / AudioEnergyExtractor.ENERGY_INTERVAL_MS).toInt()
                                    if (bucketIndex in curve.indices) {
                                        val curr = curve[bucketIndex]
                                        val prev = if (bucketIndex > 0) curve[bucketIndex - 1] else curr
                                        val delta = abs(curr - prev)
                                        (curr as Float?) to delta
                                    } else {
                                        null to 0f
                                    }
                                } else {
                                    null to 0f
                                }
                            }
                            if (isLandscape) {
                                PlayerTimestampBar(
                                    displayPositionMs = displayPosition,
                                    durationMs = progressState.durationMs,
                                    isPlaying = playbackState.isPlaying,
                                    scrubFactor = scrubFactor,
                                    rowWidthFraction = 1f,
                                    recordY = null,
                                    progress = controlsAlpha,
                                    energy = currentEnergy,
                                    energyDelta = currentEnergyDelta,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = screenWidth * 0.42f, y = screenHeight * 0.50f)
                                        .padding(end = 24.dp)
                                )
                            } else {
                                PlayerTimestampBar(
                                    displayPositionMs = displayPosition,
                                    durationMs = progressState.durationMs,
                                    isPlaying = playbackState.isPlaying,
                                    scrubFactor = scrubFactor,
                                    rowWidthFraction = rowWidthFraction,
                                    recordY = recordY,
                                    progress = controlsAlpha,
                                    energy = currentEnergy,
                                    energyDelta = currentEnergyDelta,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }

                        val containerSize = recordSize * 1.15f
                        val sizeDiff = (containerSize - recordSize) / 2

                        val isExpanded = progress > 0.9f
                        val containerWidth = screenWidth - startPadding - endPadding
                        val defaultRecordX = (containerWidth - recordSize) / 2
                        val targetRecordX = recordX - startPadding
                        val offsetX = if (isExpanded) (targetRecordX - defaultRecordX) else (targetRecordX - sizeDiff)
                        val offsetY = recordY - sizeDiff

                        // The Record Element(s)
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isExpanded) {
                                        Modifier
                                            .fillMaxWidth()
                                            .height(containerSize)
                                    } else {
                                        Modifier.size(containerSize)
                                    }
                                )
                                .offset {
                                    IntOffset(
                                        x = with(density) { offsetX.toPx() }.roundToInt(),
                                        y = with(density) { offsetY.toPx() }.roundToInt()
                                    )
                                }
                                .onGloballyPositioned { coords ->
                                    onMiniPlayerPositioned?.invoke(coords.positionInRoot())
                                }
                        ) {
                            if (isExpanded) {
                                @Suppress("DEPRECATION")
                                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = !showQueue && queueProgress == 0f && !showLyrics && !showArtistsDialog,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(boundaryNestedScrollConnection)
                                            .pointerInput(artworkSize, recordSize) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                                        val localCenter = Offset(size.width / 2f, size.height / 2f)
                                                        val distFromCenter = (down.position - localCenter).getDistance()
                                                        val artworkRadiusPx = with(density) { (artworkSize / 2).toPx() }
                                                        val recordRadiusPx = with(density) { (recordSize / 2).toPx() }

                                                        if (distFromCenter > artworkRadiusPx && queueProgress == 0f && !showQueue) {
                                                            val isVinylRing = distFromCenter <= recordRadiusPx
                                                            val pointerId = down.id
                                                            var isDragging = false
                                                            var wasPlayingOnDragStart = false
                                                            var lastAngle = calculateAngle(
                                                                down.position,
                                                                localCenter
                                                            )

                                                            while (true) {
                                                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                                                if (change.pressed) {
                                                                    val dragAmount = (change.position - down.position).getDistance()
                                                                    val touchSlop = viewConfiguration.touchSlop

                                                                    if (!isDragging) {
                                                                        if (dragAmount > touchSlop) {
                                                                            isDragging = true
                                                                            change.consume()
                                                                            if (isVinylRing) {
                                                                                wasPlayingOnDragStart = currentPlaybackState.isPlaying
                                                                                draggedPosition = currentProgressState.positionMs
                                                                                hapticAccumulator = 0f
                                                                                invalidHapticAccumulator = 0f
                                                                                lastAngle =
                                                                                    calculateAngle(
                                                                                        change.position,
                                                                                        localCenter
                                                                                    )
                                                                                if (wasPlayingOnDragStart) currentPlaybackControls.pause()
                                                                            }
                                                                        }
                                                                    } else {
                                                                        change.consume()
                                                                        if (isVinylRing) {
                                                                            val currentCenter = Offset(size.width / 2f, size.height / 2f)
                                                                            val currentAngle =
                                                                                calculateAngle(
                                                                                    change.position,
                                                                                    currentCenter
                                                                                )
                                                                            var delta = currentAngle - lastAngle
                                                                            if (delta > 180f) delta -= 360f
                                                                            if (delta < -180f) delta += 360f

                                                                            val positionDelta = (delta / 360f) * 60000f
                                                                            val unconstrainedPosition = draggedPosition + positionDelta.toLong()
                                                                            val minPos = 0L
                                                                            val maxPos = currentProgressState.durationMs

                                                                            if (unconstrainedPosition in minPos..maxPos) {
                                                                                visualRotation += delta
                                                                                draggedPosition = unconstrainedPosition
                                                                                invalidHapticAccumulator = 0f

                                                                                hapticAccumulator += abs(delta)
                                                                                if (hapticAccumulator >= 10f) {
                                                                                    view.performHapticFeedback(
                                                                                        HapticFeedbackConstants.CLOCK_TICK
                                                                                    )
                                                                                    hapticAccumulator = 0f
                                                                                }
                                                                            } else {
                                                                                val boundaryPos = if (unconstrainedPosition < minPos) minPos else maxPos
                                                                                val posDeltaToBoundary = (boundaryPos - draggedPosition).toFloat()
                                                                                val validFraction = if (positionDelta != 0f) {
                                                                                    (posDeltaToBoundary / positionDelta).coerceIn(0f, 1f)
                                                                                } else {
                                                                                    0f
                                                                                }

                                                                                val validDelta = delta * validFraction
                                                                                val invalidDelta = delta - validDelta

                                                                                visualRotation += validDelta
                                                                                draggedPosition = boundaryPos

                                                                                val invalidDegrees = abs(invalidDelta)
                                                                                if (invalidHapticAccumulator == 0f || invalidHapticAccumulator + invalidDegrees >= 12f) {
                                                                                    view.performHapticFeedback(HapticFeedbackConstantsCompat.REJECT)
                                                                                    invalidHapticAccumulator = if (invalidHapticAccumulator == 0f) 0.001f else ((invalidHapticAccumulator + invalidDegrees) % 12f)
                                                                                } else {
                                                                                    invalidHapticAccumulator += invalidDegrees
                                                                                }
                                                                            }

                                                                            lastAngle = currentAngle
                                                                            isDraggingScrub = true
                                                                        }
                                                                    }
                                                                } else {
                                                                    // Released
                                                                    if (isDragging && isVinylRing) {
                                                                        isDraggingScrub = false
                                                                        currentPlaybackControls.onSeek(draggedPosition)
                                                                        if (wasPlayingOnDragStart) currentPlaybackControls.play()
                                                                    }
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) { page ->
                                        val pageArtworkUri = when {
                                            page == currentTrackPageIndex -> playbackState.artworkUri
                                            hasPrevPage && page == currentTrackPageIndex - 1 -> prevArtworkUri
                                            hasNextPage && page == currentTrackPageIndex + 1 -> nextArtworkUri
                                            else -> null
                                        }

                                        val rawOffset = boundaryOffsetAnimatable.value
                                        val slideTranslationX = rawOffset.coerceIn(-maxSlidePx, maxSlidePx)
                                        val excessPx = (abs(rawOffset) - maxSlidePx).coerceAtLeast(0f)
                                        val stretchScaleX = 1f + (excessPx / 3200f).coerceAtMost(0.05f)

                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RecordSurface(
                                                artworkUri = pageArtworkUri,
                                                recordSize = recordSize,
                                                artworkSize = artworkSize,
                                                rotation = if (page == currentTrackPageIndex) visualRotation else 0f,
                                                overlayAlpha = if (page == currentTrackPageIndex) overlayAlpha else 0f,
                                                modifier = Modifier
                                                    .size(containerSize)
                                                    .pointerInput(artworkSize) {
                                                        detectTapGestures(
                                                            onPress = { pressOffset ->
                                                                val localCenter = Offset(size.width / 2f, size.height / 2f)
                                                                val distFromCenter = (pressOffset - localCenter).getDistance()
                                                                val artworkRadiusPx = with(density) { (artworkSize / 2).toPx() }
                                                                if (distFromCenter <= artworkRadiusPx) {
                                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                                    scale.animateTo(0.96f, animationSpec = tween(100))
                                                                    try {
                                                                        awaitRelease()
                                                                        currentPlaybackControls.onPlayPause()
                                                                    } finally {
                                                                        scale.animateTo(
                                                                            1.0f,
                                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .graphicsLayer {
                                                        if (page == currentTrackPageIndex && rawOffset != 0f) {
                                                            translationX = slideTranslationX
                                                            scaleX = stretchScaleX
                                                        }
                                                    }
                                                    .scale(scale.value * 0.85f),
                                                isScrubbing = page == currentTrackPageIndex && isDraggingScrub,
                                                isDraggingArtwork = page == currentTrackPageIndex && isDraggingArtwork
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RecordSurface(
                                        artworkUri = playbackState.artworkUri,
                                        recordSize = recordSize,
                                        artworkSize = artworkSize,
                                        rotation = visualRotation,
                                        overlayAlpha = 0f,
                                        modifier = Modifier
                                            .size(containerSize)
                                            .pointerInput(artworkSize) {
                                                detectTapGestures(
                                                    onPress = { pressOffset ->
                                                        val localCenter = Offset(size.width / 2f, size.height / 2f)
                                                        val distFromCenter = (pressOffset - localCenter).getDistance()
                                                        val artworkRadiusPx = with(density) { (artworkSize / 2).toPx() }
                                                        if (distFromCenter <= artworkRadiusPx) {
                                                            currentPlaybackControls.onPlayPause()
                                                        }
                                                    }
                                                )
                                            }
                                            .scale(scale.value * 0.85f),
                                        isScrubbing = false,
                                        isDraggingArtwork = false
                                    )
                                }
                            }
                        }

                        // Lyrics Overlay
                        AnimatedVisibility(
                            visible = showLyrics,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            LyricsScreen(
                                lyricsState = lyricsState,
                                playbackState = playbackState,
                                progressState = progressState,
                                actions = lyricsControls,
                                onSeek = { playbackControls.onSeek(it) }
                            )
                        }

                        // Queue Overlay Sheet
                        if (queueProgress > 0f || showQueue) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = queueProgress }
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        enabled = queueProgress > 0f
                                    ) {
                                        scope.launch {
                                            queueAnchoredDraggableState.animateTo(DragValue.COLLAPSED)
                                        }
                                    }
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset { IntOffset(0, queueOffset.roundToInt()) }
                                    .anchoredDraggable(queueAnchoredDraggableState, Orientation.Vertical),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 8.dp
                            ) {
                                QueueBottomSheetContent(
                                    queueState = queueState,
                                    actions = queueControls,
                                    isShuffleOn = playbackState.isShuffleOn,
                                    onShuffleToggle = { playbackControls.onShuffleToggle() },
                                    onDismiss = {
                                        scope.launch {
                                            queueAnchoredDraggableState.animateTo(DragValue.COLLAPSED)
                                        }
                                    }
                                )
                            }
                        }

                        // Song Artists Dialog
                        if (showArtistsDialog) {
                            val artistNames = remember(playbackState.artist, playbackState.trackTitle) {
                                collectArtistNames(playbackState.artist, playbackState.trackTitle)
                            }
                            SongArtistsDialog(
                                trackTitle = playbackState.trackTitle,
                                artistNames = artistNames,
                                onDismissRequest = { showArtistsDialog = false },
                                onArtistClick = { selectedArtistName ->
                                    showArtistsDialog = false
                                    val intent = Intent(context, ArtistDetailActivity::class.java).apply {
                                        putExtra(
                                            ArtistDetailActivity.EXTRA_ARTIST_ID,
                                            selectedArtistName.toArtistId()
                                        )
                                    }
                                    launchActivityWithScaleUp(context, intent, view)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



private val samplePlaybackControls = object : PlaybackControls {
    override fun onPlayPause() {}
    override fun play() {}
    override fun pause() {}
    override fun onNext() {}
    override fun onPrevious() {}
    override fun onSeek(positionMs: Long) {}
    override fun onShuffleToggle() {}
    override fun onRepeatToggle() {}
}

private val sampleQueueControls = object : QueueControls {
    override fun addToQueue(mediaItem: MediaItem) {}
    override fun onSkipToQueueItem(index: Int) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {}
}

private val sampleLyricsControls = object : LyricsControls {
    override fun onLyricsSearch(query: String) {}
}

private val samplePlaybackState = PlaybackUiState(
    trackTitle = "Midnight City",
    artist = "M83",
    album = "Hurry Up, We're Dreaming",
    albumId = "sample_album_1",
    isPlaying = true,
    hasNext = true,
    hasPrevious = true
)

private val sampleProgressState = PlaybackProgressState(
    positionMs = 125000L,
    durationMs = 243000L
)

private val sampleQueueState = QueueUiState()

@Preview(showBackground = true, name = "Portrait Phone Expanded", device = "spec:width=411dp,height=891dp,orientation=portrait")
@Composable
fun ExpandablePlayerPortraitExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Landscape Phone Expanded", device = "spec:width=891dp,height=411dp,orientation=landscape")
@Composable
fun ExpandablePlayerLandscapeExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Foldable / Tablet Expanded", device = "spec:width=800dp,height=800dp")
@Composable
fun ExpandablePlayerFoldableExpandedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Playing State", device = "id:pixel_7")
@Composable
fun ExpandablePlayerPlayingPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(isPlaying = true),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Paused State", device = "id:pixel_7")
@Composable
fun ExpandablePlayerPausedStatePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(isPlaying = false),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Short / Medium Title", device = "id:pixel_7")
@Composable
fun ExpandablePlayerShortTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(trackTitle = "Midnight City"),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Long Multi-line Title", device = "id:pixel_7")
@Composable
fun ExpandablePlayerLongTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(
                    trackTitle = "Supercalifragilisticexpialidocious Long Track Title That Needs Auto Resizing To Fit On Two Lines"
                ),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Multiple Artists", device = "id:pixel_7")
@Composable
fun ExpandablePlayerMultipleArtistsPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(
                    artist = "M83, Anthony Gonzalez, Morgan Kibby"
                ),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Energy Waveform", device = "id:pixel_7")
@Composable
fun ExpandablePlayerEnergyWaveformPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState.copy(playbackWave = PlaybackWaveType.ENERGY),
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Theme", device = "id:pixel_7")
@Composable
fun ExpandablePlayerLightThemePreview() {
    MusicPlayerTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme", device = "id:pixel_7")
@Composable
fun ExpandablePlayerDarkThemePreview() {
    MusicPlayerTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                playbackState = samplePlaybackState,
                progressState = sampleProgressState,
                queueState = sampleQueueState,
                playbackControls = samplePlaybackControls,
                queueControls = sampleQueueControls,
                lyricsControls = sampleLyricsControls,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}
