package io.github.vinaygaykar.mp1.ui.nowplaying

import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.vinaygaykar.mp1.playback.PlaybackControls
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.library.DragValue
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ColorStep(
    val color: Color,
    val isReverse: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Stable
class NowPlayingScreenState(
    val density: Density,
    val scope: CoroutineScope,
    val view: View,
    val anchoredDraggableState: AnchoredDraggableState<DragValue>,
    val queueAnchoredDraggableState: AnchoredDraggableState<DragValue>,
    val pagerState: PagerState,
    val boundaryNestedScrollConnection: NestedScrollConnection,
    val boundaryOffsetAnimatable: Animatable<Float, AnimationVector1D>,
    val expandAnimProgress: Animatable<Float, AnimationVector1D>,
    val animatableBulge: Animatable<Float, AnimationVector1D>,
    val rippleProgress: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>,
    val initialExpanded: Boolean,
    val peekHeightPx: Float,
    val screenHeightPx: Float,
    val totalBottomGapPx: Float
) {
    val totalRange: Float
        get() = screenHeightPx - peekHeightPx - totalBottomGapPx

    val defaultOffset: Float
        get() = if (initialExpanded) 0f else totalRange

    val dragOffset: Float
        get() = try {
            anchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            defaultOffset
        }

    val progress: Float
        get() = if (totalRange > 0) ((totalRange - dragOffset.coerceAtLeast(0f)) / totalRange).coerceIn(0f, 1f) else 0f

    val queueOffset: Float
        get() = try {
            queueAnchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            if (showQueue) 0f else screenHeightPx
        }

    val queueProgress: Float
        get() = if (screenHeightPx > 0f) {
            ((screenHeightPx - queueOffset.coerceAtLeast(0f)) / screenHeightPx).coerceIn(0f, 1f)
        } else {
            if (showQueue) 1f else 0f
        }

    val effectiveExpandProgress: Float
        get() = if (lastTrackTitle.isEmpty() && expandAnimProgress.value == 1f) 0f else expandAnimProgress.value

    var lastTrackTitle by mutableStateOf("")
    var isScreenActive by mutableStateOf(true)

    // Dynamic theme colors
    var playerColorScheme by mutableStateOf<ColorScheme?>(null)
    var currentColorScheme by mutableStateOf(ColorScheme(
        primary = Color.Unspecified, onPrimary = Color.Unspecified, primaryContainer = Color.Unspecified, onPrimaryContainer = Color.Unspecified,
        inversePrimary = Color.Unspecified, secondary = Color.Unspecified, onSecondary = Color.Unspecified, secondaryContainer = Color.Unspecified,
        onSecondaryContainer = Color.Unspecified, tertiary = Color.Unspecified, onTertiary = Color.Unspecified, tertiaryContainer = Color.Unspecified,
        onTertiaryContainer = Color.Unspecified, background = Color.Unspecified, onBackground = Color.Unspecified, surface = Color.Unspecified,
        onSurface = Color.Unspecified, surfaceVariant = Color.Unspecified, onSurfaceVariant = Color.Unspecified, surfaceTint = Color.Unspecified,
        inverseSurface = Color.Unspecified, inverseOnSurface = Color.Unspecified, error = Color.Unspecified, onError = Color.Unspecified,
        errorContainer = Color.Unspecified, onErrorContainer = Color.Unspecified, outline = Color.Unspecified, outlineVariant = Color.Unspecified,
        scrim = Color.Unspecified
    ))
    var targetBgColor by mutableStateOf(Color.Unspecified)

    // Song change color spread animation state
    var bgColorPair by mutableStateOf<Pair<Color, Color>?>(null)
    var isReverseRipple by mutableStateOf(false)
    var displayedIndexState by mutableIntStateOf(-1)
    var displayedColorState by mutableStateOf<Color?>(null)
    var prevQueueState by mutableStateOf<List<MediaItem>?>(null)
    var lastAnimatedTrackKey by mutableStateOf<String?>(null)

    val pendingColorQueue = mutableStateListOf<ColorStep>()

    var currentTrackPageIndex by mutableIntStateOf(0)
    var totalPageCount by mutableIntStateOf(1)
    var hasPrevPage by mutableStateOf(false)
    var hasNextPage by mutableStateOf(false)
    var prevArtworkUri by mutableStateOf<Uri?>(null)
    var nextArtworkUri by mutableStateOf<Uri?>(null)

    var isDraggingScrub by mutableStateOf(false)
    var isDraggingArtwork by mutableStateOf(false)
    var draggedPosition by mutableLongStateOf(0L)
    var hapticAccumulator by mutableFloatStateOf(0f)
    var invalidHapticAccumulator by mutableFloatStateOf(0f)

    var visualRotation by mutableFloatStateOf(0f)
    var showLyrics by mutableStateOf(false)
    var showQueue by mutableStateOf(false)
    var showArtistsDialog by mutableStateOf(false)

    fun collapse() {
        scope.launch { anchoredDraggableState.animateTo(DragValue.COLLAPSED) }
    }

    fun expand() {
        scope.launch { anchoredDraggableState.animateTo(DragValue.EXPANDED) }
    }

    fun collapseQueue() {
        scope.launch { queueAnchoredDraggableState.animateTo(DragValue.COLLAPSED) }
    }

    fun expandQueue() {
        scope.launch { queueAnchoredDraggableState.animateTo(DragValue.EXPANDED) }
    }
}

@Composable
fun rememberNowPlayingScreenState(
    playbackState: PlaybackUiState,
    queueState: QueueUiState,
    playbackControls: PlaybackControls,
    peekHeight: Dp,
    screenHeightPx: Float,
    totalBottomGapPx: Float,
    initialExpanded: Boolean,
    bulgeTrigger: Long
): NowPlayingScreenState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val peekHeightPx = with(density) { peekHeight.toPx() }

    val anchors = remember(screenHeightPx, peekHeightPx, totalBottomGapPx) {
        DraggableAnchors {
            DragValue.COLLAPSED at screenHeightPx - peekHeightPx - totalBottomGapPx
            DragValue.EXPANDED at 0f
        }
    }

    val splineBasedDecay = rememberSplineBasedDecay<Float>()

    var stateRef by remember { mutableStateOf<NowPlayingScreenState?>(null) }

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
                val s = stateRef
                if (s != null) {
                    !((s.showLyrics || s.showQueue || s.showArtistsDialog) && newValue == DragValue.COLLAPSED)
                } else true
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

    val expandAnimProgress = remember { Animatable(1f) }

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

    val fallbackColorScheme = MaterialTheme.colorScheme
    val currentColorScheme = playerColorScheme ?: fallbackColorScheme
    val targetBgColor = currentColorScheme.primaryContainer

    val rippleProgress = remember { Animatable(0f) }

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

    val scale = remember { Animatable(1f) }

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

    val state = remember(density, scope, view) {
        NowPlayingScreenState(
            density = density,
            scope = scope,
            view = view,
            anchoredDraggableState = anchoredDraggableState,
            queueAnchoredDraggableState = queueAnchoredDraggableState,
            pagerState = pagerState,
            boundaryNestedScrollConnection = boundaryNestedScrollConnection,
            boundaryOffsetAnimatable = boundaryOffsetAnimatable,
            expandAnimProgress = expandAnimProgress,
            animatableBulge = animatableBulge,
            rippleProgress = rippleProgress,
            scale = scale,
            initialExpanded = initialExpanded,
            peekHeightPx = peekHeightPx,
            screenHeightPx = screenHeightPx,
            totalBottomGapPx = totalBottomGapPx
        )
    }
    stateRef = state

    state.isScreenActive = isScreenActive
    state.playerColorScheme = playerColorScheme
    state.currentColorScheme = currentColorScheme
    state.targetBgColor = targetBgColor

    state.currentTrackPageIndex = currentTrackPageIndex
    state.totalPageCount = totalPageCount
    state.hasPrevPage = hasPrevPage
    state.hasNextPage = hasNextPage
    state.prevArtworkUri = prevArtworkUri
    state.nextArtworkUri = nextArtworkUri

    // Initial pill expansion animation trigger
    LaunchedEffect(playbackState.trackTitle) {
        if (state.lastTrackTitle.isEmpty() && playbackState.trackTitle.isNotEmpty()) {
            expandAnimProgress.snapTo(0f)
            delay(1000L)
            expandAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        } else if (playbackState.trackTitle.isEmpty()) {
            expandAnimProgress.snapTo(0f)
        }
        state.lastTrackTitle = playbackState.trackTitle
    }

    // Song change color spread producer
    val currentTrackKey = remember(queueState.currentIndex, playbackState.trackTitle) {
        "${queueState.currentIndex}_${playbackState.trackTitle}"
    }

    var lastAnimatedTrackKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTrackKey, targetBgColor) {
        val newIndex = queueState.currentIndex
        val prevIndex = state.displayedIndexState
        val newQueue = queueState.items
        val prevQueue = state.prevQueueState

        val isSameQueue = prevQueue != null &&
                prevQueue.size == newQueue.size &&
                prevQueue.indices.all { i -> prevQueue[i].mediaId == newQueue[i].mediaId }

        var currentIsReverse = state.isReverseRipple
        if (newIndex != prevIndex && newIndex != -1) {
            currentIsReverse = isSameQueue && prevIndex != -1 && newIndex < prevIndex
            state.isReverseRipple = currentIsReverse
            state.displayedIndexState = newIndex
            state.prevQueueState = newQueue
        }

        val isInitialLoad = lastAnimatedTrackKey == null || state.displayedColorState == null
        val isSongChanged = lastAnimatedTrackKey != null && lastAnimatedTrackKey != currentTrackKey
        val isArtworkLoading = playbackState.artworkUri != null && playbackState.seedColor == null

        if (isInitialLoad) {
            state.displayedColorState = targetBgColor
            lastAnimatedTrackKey = currentTrackKey
        } else if (isSongChanged) {
            val startColor = state.pendingColorQueue.lastOrNull()?.color ?: state.displayedColorState ?: targetBgColor
            if (startColor != targetBgColor && !isArtworkLoading) {
                while (state.pendingColorQueue.size >= 4) {
                    state.pendingColorQueue.removeAt(0)
                }
                state.pendingColorQueue.add(ColorStep(targetBgColor, currentIsReverse))
                lastAnimatedTrackKey = currentTrackKey
            } else if (startColor == targetBgColor && !isArtworkLoading) {
                lastAnimatedTrackKey = currentTrackKey
            }
        } else {
            if (state.pendingColorQueue.isEmpty() && state.bgColorPair == null) {
                state.displayedColorState = targetBgColor
            }
        }
    }

    // Consumer Worker: Processes pending color transitions sequentially
    LaunchedEffect(Unit) {
        while (true) {
            if (state.pendingColorQueue.isEmpty()) {
                snapshotFlow { state.pendingColorQueue.size }.first { it > 0 }
            }
            if (state.pendingColorQueue.isNotEmpty()) {
                val nextStep = state.pendingColorQueue.removeAt(0)
                val startColor = state.displayedColorState ?: targetBgColor
                if (startColor != nextStep.color) {
                    state.bgColorPair = startColor to nextStep.color
                    state.isReverseRipple = nextStep.isReverse

                    val duration = if (state.pendingColorQueue.isNotEmpty()) 350 else 800
                    state.rippleProgress.snapTo(0f)
                    state.rippleProgress.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))

                    state.displayedColorState = nextStep.color
                    state.bgColorPair = null
                } else {
                    state.displayedColorState = nextStep.color
                }
            }
        }
    }

    // Rotation frame updates when playing
    LaunchedEffect(playbackState.isPlaying) {
        if (playbackState.isPlaying) {
            var lastFrameTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
                    state.visualRotation += deltaMs * 0.12f
                    lastFrameTime = frameTime
                }
            }
        }
    }

    LaunchedEffect(playbackState.trackTitle, playbackState.artist) {
        state.visualRotation = 0f
    }

    // Queue sheet state flow
    LaunchedEffect(state.showQueue) {
        if (state.showQueue) {
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
                    if (state.showQueue) state.showQueue = false
                } else if (current == DragValue.EXPANDED || target == DragValue.EXPANDED) {
                    if (!state.showQueue) state.showQueue = true
                }
            }
    }

    return state
}
