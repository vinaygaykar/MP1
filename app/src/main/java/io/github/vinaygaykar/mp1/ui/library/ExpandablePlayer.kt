package io.github.vinaygaykar.mp1.ui.library

import android.view.HapticFeedbackConstants
import android.view.View
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.ui.artist.ArtistDetailActivity
import io.github.vinaygaykar.mp1.ui.faces.VinylFace
import io.github.vinaygaykar.mp1.ui.theme.DynamicThemeHelper
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.toArtistId
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
    val recordOffsetX = remember { Animatable(0f) }

    var isDraggingScrub by remember { mutableStateOf(false) }
    var isDraggingArtwork by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableLongStateOf(0L) }
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var hapticAccumulator by remember { mutableFloatStateOf(0f) }

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
        targetValue = if (isDraggingScrub) 0.9f else 0.4f,
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

        BackHandler(enabled = anchoredDraggableState.currentValue != DragValue.COLLAPSED) {
            if (showArtistsDialog) {
                showArtistsDialog = false
            } else if (showLyrics) {
                showLyrics = false
            } else if (showQueue) {
                showQueue = false
            } else {
                scope.launch { anchoredDraggableState.animateTo(DragValue.COLLAPSED) }
            }
        }

        val defaultOffset =
            if (initialExpanded) 0f else (screenHeightPx - peekHeightPx - totalBottomGapPx)
        val dragOffset = try {
            anchoredDraggableState.requireOffset()
        } catch (_: Exception) {
            defaultOffset
        }
        val totalRange = screenHeightPx - peekHeightPx - totalBottomGapPx
        val progress =
            if (totalRange > 0) ((totalRange - dragOffset.coerceAtLeast(0f)) / totalRange).coerceIn(
                0f,
                1f
            ) else 0f
        val currentProgress by rememberUpdatedState(progress)

        LaunchedEffect(progress) {
            onProgressUpdate(progress)
        }

        val vinylFace = remember { VinylFace() }

        val nextArtworkUri = remember(state.queue, state.currentIndex) {
            if (state.currentIndex >= 0) {
                state.queue.getOrNull(state.currentIndex + 1)?.mediaMetadata?.artworkUri
            } else null
        }
        val prevArtworkUri = remember(state.queue, state.currentIndex) {
            if (state.currentIndex >= 0) {
                state.queue.getOrNull(state.currentIndex - 1)?.mediaMetadata?.artworkUri
            } else null
        }

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
                                x = (localRecordX + recordSize / 2).toPx() + recordOffsetX.value,
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
                            .pointerInput(progress, showQueue, showLyrics) {
                                if (progress > 0.8f && !showQueue && !showLyrics) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var totalY = 0f
                                        val pointerId = down.id
                                        do {
                                            val event = awaitPointerEvent()
                                            val pointer =
                                                event.changes.find { it.id == pointerId } ?: break
                                            if (pointer.pressed) {
                                                val deltaY = pointer.positionChange().y
                                                if (deltaY < 0f) {
                                                    totalY += deltaY
                                                    if (totalY < -50f) {
                                                        showQueue = true
                                                        pointer.consume()
                                                        break
                                                    }
                                                } else if (deltaY > 10f) {
                                                    break
                                                }
                                            } else {
                                                break
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            }
                    ) {
                        // Expanded Content
                        if (progress > 0f) {
                            vinylFace.Render(
                                state = state,
                                actions = actions,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(
                                        progress,
                                        state.hasNext,
                                        state.hasPrevious,
                                        nextArtworkUri,
                                        prevArtworkUri
                                    ) {
                                        if (progress > 0.9f) {
                                            detectHorizontalDragGestures(
                                                onHorizontalDrag = { change, dragAmount ->
                                                    scope.launch {
                                                        recordOffsetX.snapTo(recordOffsetX.value + dragAmount)
                                                    }
                                                    change.consume()
                                                },
                                                onDragEnd = {
                                                    scope.launch {
                                                        performTrackSwipe(
                                                            recordOffsetX,
                                                            screenWidthPx,
                                                            state,
                                                            actions,
                                                            view
                                                        )
                                                    }
                                                },
                                                onDragCancel = {
                                                    scope.launch { recordOffsetX.animateTo(0f) }
                                                }
                                            )
                                        }
                                    },
                                showRecord = false,
                                onArtistClick = {
                                    showArtistsDialog = true
                                }
                            )
                        }

                        // MiniPlayer Content
                        if (progress < 1f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(peekHeight)
                                    .alpha(1f - progress)
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
                        // Top Collapse Handle
                        if (progress > 0.8f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .alpha(progress)
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
                                    .alpha(progress)
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
                                    .alpha(progress)
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
                                    .alpha(progress)
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
                        if (progress > 0f) {
                            val displayPosition =
                                if (isDraggingScrub) draggedPosition else state.positionMs
                            PlayerTimestampBar(
                                displayPositionMs = displayPosition,
                                durationMs = state.durationMs,
                                isPlaying = state.isPlaying,
                                scrubFactor = scrubFactor,
                                rowWidthFraction = rowWidthFraction,
                                recordY = recordY,
                                progress = progress,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }

                        val containerSize = recordSize * 1.15f
                        val sizeDiff = (containerSize - recordSize) / 2

                        // The Record Element(s)
                        Box(
                            modifier = Modifier
                                .size(containerSize)
                                .offset {
                                    val localRecordX = recordX - startPadding - sizeDiff
                                    IntOffset(
                                        x = (with(density) { localRecordX.toPx() } + recordOffsetX.value).roundToInt(),
                                        y = with(density) { (recordY - sizeDiff).toPx() }.roundToInt()
                                    )
                                }
                                .scale(scale.value * 0.85f)
                                .onGloballyPositioned { coords ->
                                    onMiniPlayerPositioned?.invoke(coords.positionInRoot())
                                }
                                /*
                                 * Previous unified gesture handler for track swiping and scrubbing
                                 * on the entire record surface (both album art and outer circle).
                                 *
                                .pointerInput(
                                    Unit,
                                    state.hasNext,
                                    state.hasPrevious,
                                    nextArtworkUri,
                                    prevArtworkUri
                                ) {
                                    var dragStarted = false
                                    var dragMode =
                                        0 // 0: undecided, 1: horizontal (skip), 2: circular (scrub)
                                    var wasPlayingOnDragStart = false

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (currentProgress > 0.9f) {
                                                dragStarted = true
                                                dragMode = 0
                                                wasPlayingOnDragStart = currentState.isPlaying
                                                draggedPosition = currentState.positionMs
                                                hapticAccumulator = 0f
                                                val localCenter =
                                                    Offset(size.width / 2f, size.height / 2f)
                                                lastAngle = calculateAngle(offset, localCenter)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (dragStarted) {
                                                val localCenter =
                                                    Offset(size.width / 2f, size.height / 2f)
                                                val currentAngle =
                                                    calculateAngle(change.position, localCenter)
                                                var delta = currentAngle - lastAngle
                                                if (delta > 180f) delta -= 360f
                                                if (delta < -180f) delta += 360f

                                                if (dragMode == 0) {
                                                    val absX = abs(dragAmount.x)
                                                    val absY = abs(dragAmount.y)
                                                    if (dragAmount.y > absX * 2f && abs(delta) < 1.0f) {
                                                        dragStarted = false
                                                    } else if (absX > absY * 2.5f && abs(delta) < 1.5f) {
                                                        dragMode = 1
                                                        if (wasPlayingOnDragStart) currentActions.pause()
                                                    } else if (abs(delta) > 1.0f || (absX > 4f && absY > 4f)) {
                                                        dragMode = 2
                                                        isDraggingScrub = true
                                                        if (wasPlayingOnDragStart) currentActions.pause()
                                                    }
                                                }

                                                if (dragMode == 1) {
                                                    scope.launch {
                                                        recordOffsetX.snapTo(recordOffsetX.value + dragAmount.x)
                                                    }
                                                } else if (dragMode == 2) {
                                                    visualRotation += delta
                                                    val positionDelta = (delta / 360f) * 60000f
                                                    draggedPosition =
                                                        (draggedPosition + positionDelta.toLong()).coerceIn(
                                                            0L,
                                                            currentState.durationMs
                                                        )
                                                    hapticAccumulator += abs(delta)
                                                    if (hapticAccumulator >= 10f) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.CLOCK_TICK
                                                        )
                                                        hapticAccumulator = 0f
                                                    }
                                                    lastAngle = currentAngle
                                                }
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (dragStarted) {
                                                if (dragMode == 1) {
                                                    scope.launch {
                                                        performTrackSwipe(
                                                            recordOffsetX,
                                                            screenWidthPx,
                                                            state,
                                                            actions,
                                                            view
                                                        )
                                                        if (wasPlayingOnDragStart) currentActions.play()
                                                    }
                                                } else if (dragMode == 2) {
                                                    isDraggingScrub = false
                                                    currentActions.onSeek(draggedPosition)
                                                    if (wasPlayingOnDragStart) currentActions.play()
                                                }
                                            }
                                            dragStarted = false
                                        },
                                        onDragCancel = {
                                            if (dragStarted) {
                                                scope.launch { recordOffsetX.animateTo(0f) }
                                                isDraggingScrub = false
                                            }
                                            dragStarted = false
                                        }
                                    )
                                }
                                */
                                .pointerInput(
                                    Unit,
                                    state.hasNext,
                                    state.hasPrevious,
                                    nextArtworkUri,
                                    prevArtworkUri,
                                    artworkSize
                                ) {
                                    var dragStarted = false
                                    var isTouchOnAlbumArt = false
                                    var dragMode =
                                        0 // 0: undecided, 1: horizontal track swipe (album art), 2: circular scrub (outer circle)
                                    var wasPlayingOnDragStart = false

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (currentProgress > 0.9f) {
                                                val localCenter =
                                                    Offset(size.width / 2f, size.height / 2f)
                                                val distFromCenter =
                                                    (offset - localCenter).getDistance()
                                                val artworkRadiusPx =
                                                    with(density) { (artworkSize / 2).toPx() }

                                                dragStarted = true
                                                isTouchOnAlbumArt =
                                                    distFromCenter <= artworkRadiusPx
                                                dragMode = 0
                                                wasPlayingOnDragStart = currentState.isPlaying
                                                draggedPosition = currentState.positionMs
                                                hapticAccumulator = 0f
                                                lastAngle = calculateAngle(offset, localCenter)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (dragStarted) {
                                                val localCenter =
                                                    Offset(size.width / 2f, size.height / 2f)
                                                val currentAngle =
                                                    calculateAngle(change.position, localCenter)
                                                var delta = currentAngle - lastAngle
                                                if (delta > 180f) delta -= 360f
                                                if (delta < -180f) delta += 360f

                                                if (dragMode == 0) {
                                                    val absX = abs(dragAmount.x)
                                                    val absY = abs(dragAmount.y)

                                                    if (isTouchOnAlbumArt) {
                                                        // Album art object: ONLY left/right track swipe is allowed. Scrubbing is disabled.
                                                        if (absY > absX * 2f && abs(delta) < 1.0f) {
                                                            dragStarted = false
                                                        } else if (absX > absY * 2.5f && abs(delta) < 1.5f) {
                                                            dragMode = 1
                                                            isDraggingArtwork = true
                                                            if (wasPlayingOnDragStart) currentActions.pause()
                                                        }
                                                    } else {
                                                        // Outer circle: ONLY scrubbing gesture is allowed. Track swiping is disabled.
                                                        if (abs(delta) > 1.0f || (absX > 4f && absY > 4f)) {
                                                            dragMode = 2
                                                            isDraggingScrub = true
                                                            if (wasPlayingOnDragStart) currentActions.pause()
                                                        }
                                                    }
                                                }

                                                if (dragMode == 1) {
                                                    // Track swiping (activated only on album art object)
                                                    scope.launch {
                                                        recordOffsetX.snapTo(recordOffsetX.value + dragAmount.x)
                                                    }
                                                } else if (dragMode == 2) {
                                                    // Scrubbing (activated only on outer circle)
                                                    visualRotation += delta
                                                    val positionDelta = (delta / 360f) * 60000f
                                                    draggedPosition =
                                                        (draggedPosition + positionDelta.toLong()).coerceIn(
                                                            0L,
                                                            currentState.durationMs
                                                        )
                                                    hapticAccumulator += abs(delta)
                                                    if (hapticAccumulator >= 10f) {
                                                        view.performHapticFeedback(
                                                            HapticFeedbackConstants.CLOCK_TICK
                                                        )
                                                        hapticAccumulator = 0f
                                                    }
                                                    lastAngle = currentAngle
                                                }
                                                change.consume()
                                            }
                                        },
                                        onDragEnd = {
                                            if (dragStarted) {
                                                if (dragMode == 1) {
                                                    isDraggingArtwork = false
                                                    scope.launch {
                                                        performTrackSwipe(
                                                            recordOffsetX,
                                                            screenWidthPx,
                                                            state,
                                                            actions,
                                                            view
                                                        )
                                                        if (wasPlayingOnDragStart) currentActions.play()
                                                    }
                                                } else if (dragMode == 2) {
                                                    isDraggingScrub = false
                                                    currentActions.onSeek(draggedPosition)
                                                    if (wasPlayingOnDragStart) currentActions.play()
                                                }
                                            }
                                            dragStarted = false
                                            isDraggingArtwork = false
                                        },
                                        onDragCancel = {
                                            if (dragStarted) {
                                                if (dragMode == 1) {
                                                    isDraggingArtwork = false
                                                    scope.launch { recordOffsetX.animateTo(0f) }
                                                } else if (dragMode == 2) {
                                                    isDraggingScrub = false
                                                }
                                            }
                                            dragStarted = false
                                            isDraggingArtwork = false
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            if (currentProgress > 0.9f) {
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
                                            } else if (currentProgress <= 0.9f) {
                                                currentActions.onPlayPause()
                                            }
                                        }
                                    )
                                }
                        ) {
                            RecordSurface(
                                artworkUri = state.artworkUri,
                                recordSize = recordSize,
                                artworkSize = artworkSize,
                                rotation = visualRotation,
                                overlayAlpha = overlayAlpha,
                                modifier = Modifier.fillMaxSize(),
                                isScrubbing = isDraggingScrub,
                                isDraggingArtwork = isDraggingArtwork
                            )

                            if (recordOffsetX.value < 0 && state.hasNext) {
                                RecordSurface(
                                    artworkUri = nextArtworkUri,
                                    recordSize = recordSize,
                                    artworkSize = artworkSize,
                                    rotation = 0f,
                                    overlayAlpha = 0f,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = screenWidth)
                                )
                            }
                            if (recordOffsetX.value > 0 && state.hasPrevious) {
                                RecordSurface(
                                    artworkUri = prevArtworkUri,
                                    recordSize = recordSize,
                                    artworkSize = artworkSize,
                                    rotation = 0f,
                                    overlayAlpha = 0f,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = -screenWidth)
                                )
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
                        if (showQueue) {
                            QueueBottomSheet(
                                state = state,
                                actions = actions,
                                onDismissRequest = { showQueue = false }
                            )
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

private suspend fun performTrackSwipe(
    recordOffsetX: Animatable<Float, *>,
    screenWidthPx: Float,
    state: PlayerUiState,
    actions: PlayerActions,
    view: View
) {
    if (recordOffsetX.value > screenWidthPx / 4) {
        if (state.hasPrevious) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            recordOffsetX.animateTo(screenWidthPx, tween(300))
            actions.onPrevious()
            recordOffsetX.snapTo(0f)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            recordOffsetX.animateTo(0f, spring())
        }
    } else if (recordOffsetX.value < -screenWidthPx / 4) {
        if (state.hasNext) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            recordOffsetX.animateTo(-screenWidthPx, tween(300))
            actions.onNext()
            recordOffsetX.snapTo(0f)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            recordOffsetX.animateTo(0f, spring())
        }
    } else {
        recordOffsetX.animateTo(0f, spring())
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

@Composable
fun SongArtistsDialog(
    trackTitle: String,
    artistNames: List<String>,
    onDismissRequest: () -> Unit,
    onArtistClick: (String) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (trackTitle.isNotBlank()) {
                            Text(
                                text = trackTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Artist List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(artistNames, key = { it }) { name ->
                        SongArtistDialogItem(
                            artistName = name,
                            onClick = { onArtistClick(name) },
                            onFetchImageUrl = { artistName, artistId ->
                                libraryViewModel.getArtistImageUrl(artistName, artistId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongArtistDialogItem(
    artistName: String,
    onClick: () -> Unit,
    onFetchImageUrl: suspend (String, String) -> String?
) {
    val artistId = remember(artistName) { artistName.toArtistId() }
    var imageUrl by remember(artistName) { mutableStateOf<String?>(null) }

    LaunchedEffect(artistName, artistId) {
        val fetchedUrl = onFetchImageUrl(artistName, artistId)
        if (!fetchedUrl.isNullOrEmpty()) {
            imageUrl = fetchedUrl
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
