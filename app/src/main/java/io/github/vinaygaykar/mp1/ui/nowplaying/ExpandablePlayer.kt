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
import io.github.vinaygaykar.mp1.domain.models.VisualizerState
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandablePlayer(
    state: PlayerUiState,
    actions: PlayerActions,
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
    var lastTrackTitle by remember { mutableStateOf(if (initialExpanded) state.trackTitle else "") }

    LaunchedEffect(state.trackTitle) {
        if (lastTrackTitle.isEmpty() && state.trackTitle.isNotEmpty()) {
            expandAnimProgress.snapTo(0f)
            delay(1000L)
            expandAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        } else if (state.trackTitle.isEmpty()) {
            expandAnimProgress.snapTo(0f)
        }
        lastTrackTitle = state.trackTitle
    }

    if (state.trackTitle.isEmpty()) return

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

    val currentActions by rememberUpdatedState(actions)
    val currentState by rememberUpdatedState(state)

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
    val playerColorScheme: ColorScheme? = remember(state.seedColor, isDark) {
        state.seedColor?.let { color ->
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

    val currentTrackKey = remember(state.currentIndex, state.trackTitle) {
        "${state.currentIndex}_${state.trackTitle}"
    }

    // Producer: Enqueues transitions on actual track changes
    LaunchedEffect(currentTrackKey, targetBgColor) {
        val newIndex = state.currentIndex
        val prevIndex = displayedIndexState
        val newQueue = state.queue
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
        val isArtworkLoading = state.artworkUri != null && state.seedColor == null

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
    val hasPrevPage = remember(state.queue.size, state.currentIndex, state.repeatMode) {
        if (state.queue.size <= 1) {
            state.repeatMode == Player.REPEAT_MODE_ALL
        } else {
            if (state.repeatMode == Player.REPEAT_MODE_ALL) {
                true
            } else {
                state.currentIndex > 0
            }
        }
    }

    val hasNextPage = remember(state.queue.size, state.currentIndex, state.repeatMode) {
        if (state.queue.size <= 1) {
            state.repeatMode == Player.REPEAT_MODE_ALL
        } else {
            if (state.repeatMode == Player.REPEAT_MODE_ALL) {
                true
            } else {
                state.currentIndex in 0 until state.queue.size - 1
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

    LaunchedEffect(state.currentIndex, state.trackTitle, hasPrevPage, hasNextPage) {
        if (pagerState.currentPage != currentTrackPageIndex) {
            pagerState.scrollToPage(currentTrackPageIndex)
        }
    }

    val targetTrackPageIndex by rememberUpdatedState(currentTrackPageIndex)

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage < targetTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            actions.onPrevious()
            if (pagerState.currentPage != targetTrackPageIndex) {
                pagerState.scrollToPage(targetTrackPageIndex)
            }
        } else if (pagerState.settledPage > targetTrackPageIndex) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            actions.onNext()
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
    var lastAngle by remember { mutableFloatStateOf(0f) }
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

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
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

    LaunchedEffect(state.trackTitle, state.artist) {
        visualRotation = 0f
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight
        val screenWidth = this.maxWidth

        val peekHeightPx = with(density) { peekHeight.toPx() }
        val screenHeightPx = with(density) { screenHeight.toPx() }
        val screenWidthPx = with(density) { screenWidth.toPx() }
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
        val activeSongId = remember(state.queue, state.currentIndex, currentTrackKey) {
            if (state.currentIndex in state.queue.indices) {
                state.queue[state.currentIndex].mediaId
            } else {
                currentTrackKey
            }
        }

        LaunchedEffect(activeSongId, isPlayerVisible, state.playbackWave) {
            currentActions.onUpdateVisualizerVisibility(isPlayerVisible)
        }

        val prevItem = remember(state.queue, state.currentIndex, hasPrevPage) {
            if (!hasPrevPage || state.queue.isEmpty()) null
            else {
                val prevIndex = if (state.currentIndex > 0) {
                    state.currentIndex - 1
                } else {
                    state.queue.size - 1
                }
                state.queue.getOrNull(prevIndex)
            }
        }
        val prevArtworkUri = remember(prevItem) { prevItem?.mediaMetadata?.artworkUri }

        val nextItem = remember(state.queue, state.currentIndex, hasNextPage) {
            if (!hasNextPage || state.queue.isEmpty()) null
            else {
                val nextIndex = if (state.currentIndex in 0 until state.queue.size - 1) {
                    state.currentIndex + 1
                } else {
                    0
                }
                state.queue.getOrNull(nextIndex)
            }
        }
        val nextArtworkUri = remember(nextItem) { nextItem?.mediaMetadata?.artworkUri }

        val currentColorScheme = playerColorScheme ?: MaterialTheme.colorScheme
        val horizontalPadding = lerp(20.dp, 0.dp, progress)

        val isPreview = LocalInspectionMode.current
        val effectiveExpandProgress = if (!isPreview && lastTrackTitle.isEmpty() && state.trackTitle.isNotEmpty() && expandAnimProgress.value == 1f) 0f else expandAnimProgress.value

        val rawStartPadding = lerp(screenWidth - 92.dp, 20.dp, effectiveExpandProgress)
        val rawEndPadding = 20.dp

        val startPadding = lerp(rawStartPadding, 0.dp, progress)
        val endPadding = lerp(rawEndPadding, 0.dp, progress)

        val cornerRadius = lerp(50.dp, 0.dp, progress)
        val sheetShadowElevation = lerp(8.dp, 0.dp, progress)
        val surfaceAlpha = 1f
        val recordSize = lerp(56.dp, screenWidth * 0.85f, progress)
        val artworkSize = lerp(56.dp, min(210.dp, recordSize * 0.6f), progress)
        val recordX = lerp(screenWidth - recordSize - 8.dp - horizontalPadding, (screenWidth - recordSize) / 2 - horizontalPadding, progress)
        val recordY = lerp(8.dp, screenHeight * 0.44f, progress)

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
                            ExpandedPlayerTrackInfo(
                                state = state,
                                modifier = Modifier.fillMaxSize(),
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
                                    state = state,
                                    actions = actions,
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

                        // Lyrics Toggle Button
                        if (progress > 0.8f) {
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
                        }

                        // Repeat Mode Toggle Button
                        if (progress > 0.8f) {
                            val (repeatIcon, repeatText) = when (state.repeatMode) {
                                1 -> Icons.Rounded.RepeatOne to "Song"
                                2 -> Icons.Rounded.RepeatOn to "List"
                                else -> Icons.Rounded.Repeat to "Off"
                            }

                            Surface(
                                onClick = { actions.onRepeatToggle() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp)
                                    .alpha(controlsAlpha)
                                    .width(110.dp)
                                    .height(48.dp),
                                shape = CircleShape,
                                color = if (state.repeatMode != 0) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f)
                                },
                                contentColor = if (state.repeatMode != 0) {
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
                        }

                        // Queue Toggle Button
                        if (progress > 0.8f) {
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

                        // Timestamp display
                        if (progress > 0.8f) {
                            val displayPosition =
                                if (isDraggingScrub) draggedPosition else state.positionMs
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
                            val currentFrequencyFrame = remember(visualizerState, displayPosition) {
                                if (visualizerState is VisualizerState.Frequency) {
                                    val curve = visualizerState.frames
                                    val bucketIndex = (displayPosition / AudioFrequencyExtractor.FREQUENCY_INTERVAL_MS).toInt()
                                    if (bucketIndex in curve.indices) {
                                        curve[bucketIndex]
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }
                            PlayerTimestampBar(
                                displayPositionMs = displayPosition,
                                durationMs = state.durationMs,
                                isPlaying = state.isPlaying,
                                scrubFactor = scrubFactor,
                                rowWidthFraction = rowWidthFraction,
                                recordY = recordY,
                                progress = controlsAlpha,
                                energy = currentEnergy,
                                energyDelta = currentEnergyDelta,
                                frequencyFrame = currentFrequencyFrame,
                                playbackWave = state.playbackWave,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
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
                                                                                wasPlayingOnDragStart = currentState.isPlaying
                                                                                draggedPosition = currentState.positionMs
                                                                                hapticAccumulator = 0f
                                                                                invalidHapticAccumulator = 0f
                                                                                lastAngle =
                                                                                    calculateAngle(
                                                                                        change.position,
                                                                                        localCenter
                                                                                    )
                                                                                if (wasPlayingOnDragStart) currentActions.pause()
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
                                                                            val maxPos = currentState.durationMs

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
                                                                        currentActions.onSeek(draggedPosition)
                                                                        if (wasPlayingOnDragStart) currentActions.play()
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
                                            page == currentTrackPageIndex -> state.artworkUri
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
                                                                        currentActions.onPlayPause()
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
                                        artworkUri = state.artworkUri,
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
                                                            currentActions.onPlayPause()
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
                                state = state,
                                actions = actions,
                                onSeek = { actions.onSeek(it) },
                                onSearch = { actions.onLyricsSearch(it) }
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
                                    state = state,
                                    actions = actions,
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
                            val artistNames = remember(state.artist, state.trackTitle) {
                                collectArtistNames(state.artist, state.trackTitle)
                            }
                            SongArtistsDialog(
                                trackTitle = state.trackTitle,
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



private val samplePlayerActions = object : PlayerActions {
    override fun onPlayPause() {}
    override fun play() {}
    override fun pause() {}
    override fun onNext() {}
    override fun onPrevious() {}
    override fun onSeek(positionMs: Long) {}
    override fun onShuffleToggle() {}
    override fun onRepeatToggle() {}
    override fun addToQueue(mediaItem: MediaItem) {}
    override fun onSkipToQueueItem(index: Int) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {}
    override fun onLyricsSearch(query: String) {}
}

private val samplePlayerState = PlayerUiState(
    trackTitle = "Midnight City",
    artist = "M83",
    album = "Hurry Up, We're Dreaming",
    albumId = "sample_album_1",
    positionMs = 125000L,
    durationMs = 243000L,
    isPlaying = true,
    hasNext = true,
    hasPrevious = true
)

@Preview(showBackground = true, name = "Expandable Player - Medium Title", device = "id:pixel_7")
@Composable
fun ExpandablePlayerMediumTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                state = samplePlayerState.copy(
                    trackTitle = "A Desolation Song"
                ),
                actions = samplePlayerActions,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expandable Player - Long Title", device = "id:pixel_7")
@Composable
fun ExpandablePlayerLongTitlePreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                state = samplePlayerState.copy(
                    trackTitle = "Supercalifragilisticexpialidocious Long Track Title That Needs Auto Resizing To Fit On Two Lines"
                ),
                actions = samplePlayerActions,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expandable Player - Paused", device = "id:pixel_7")
@Composable
fun ExpandablePlayerPausedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExpandablePlayer(
                state = samplePlayerState.copy(isPlaying = false),
                actions = samplePlayerActions,
                onProgressUpdate = {},
                initialExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expandable Player - Collapsed", heightDp = 120)
@Composable
fun ExpandablePlayerCollapsedPreview() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            ExpandablePlayer(
                state = samplePlayerState,
                actions = samplePlayerActions,
                onProgressUpdate = {}
            )
        }
    }
}
