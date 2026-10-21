package io.github.vinaygaykar.mp1.ui.library.tabs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import io.github.vinaygaykar.mp1.utils.splitArtistsByDelimiters
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Toggle flag to switch between the new overlapping zigzag card layout
 * and the legacy 2-column grid layout for fast scrolling.
 */
const val USE_NEW_FAST_GRID = true

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGridPunk(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No albums found.")
        }
        return
    }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val fastLazyListState = rememberLazyListState()

    var isScrollbarInteracting by remember { mutableStateOf(false) }
    var isVelocityFastScrolling by remember { mutableStateOf(false) }
    var isScreenTouched by remember { mutableStateOf(false) }
    var lastFastScrollTime by remember { mutableLongStateOf(0L) }

    val isFastModeActive = isScrollbarInteracting || isVelocityFastScrolling

    val minFlingVelocityPxPerSec = with(LocalDensity.current) { 600.dp.toPx() }
    val minSwipeDistancePx = with(LocalDensity.current) { 100.dp.toPx() }

    val nestedScrollConnection = remember(minFlingVelocityPxPerSec, minSwipeDistancePx) {
        object : NestedScrollConnection {
            private var lastScrollTime = 0L
            private var strokeDistance = 0f
            private var swipeCount = 0
            private var lastFlingTime = 0L

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val now = System.currentTimeMillis()

                // Reset stroke distance if finger movement paused (> 200ms)
                if (now - lastScrollTime > 200L) {
                    strokeDistance = 0f
                }
                lastScrollTime = now
                strokeDistance += abs(available.y)

                if (abs(available.y) > 0.5f && isVelocityFastScrolling) {
                    lastFastScrollTime = now
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val now = System.currentTimeMillis()
                val flingSpeed = abs(available.y)
                val timeSinceLastFling = now - lastFlingTime

                // A swipe qualifies if it covered enough physical distance (>= 100dp) and had fling velocity (>= 600dp/s)
                val isQualifiedSwipe = strokeDistance >= minSwipeDistancePx && flingSpeed >= minFlingVelocityPxPerSec

                if (isQualifiedSwipe) {
                    // Check if this is a second consecutive swipe within the double-swipe window (50ms - 650ms)
                    val isQuickSuccession = timeSinceLastFling in 50L..650L

                    if (swipeCount == 1 && isQuickSuccession) {
                        // Double swipe criteria met! Switch to fast grid.
                        isVelocityFastScrolling = true
                        lastFastScrollTime = now
                        swipeCount = 0
                    } else {
                        // First qualified swipe in the sequence
                        swipeCount = 1
                        lastFlingTime = now
                    }
                } else if (timeSinceLastFling > 650L) {
                    // Reset counter if gesture didn't qualify and time window passed
                    swipeCount = 0
                }

                strokeDistance = 0f
                return super.onPreFling(available)
            }
        }
    }

    LaunchedEffect(isVelocityFastScrolling) {
        if (isVelocityFastScrolling) {
            while (true) {
                delay(100L)
                val timeSinceFastScroll = System.currentTimeMillis() - lastFastScrollTime
                val isScrolling =
                    lazyListState.isScrollInProgress || lazyGridState.isScrollInProgress || fastLazyListState.isScrollInProgress
                if (!isScreenTouched && !isScrolling && timeSinceFastScroll > 300L) {
                    isVelocityFastScrolling = false
                    break
                }
            }
        }
    }

    val animationDuration = if (isFastModeActive) 300 else 600
    val fastScrollProgress by animateFloatAsState(
        targetValue = if (isFastModeActive) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "FastScrollProgress"
    )

    // Synchronize positions between Slow Scroll (LazyColumn) and Fast Scroll (LazyColumn or LazyVerticalGrid)
    LaunchedEffect(isFastModeActive) {
        if (isFastModeActive) {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val targetIndex = if (visibleItems.isNotEmpty()) {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2f
                    abs(itemCenter - viewportCenter)
                }?.index ?: lazyListState.firstVisibleItemIndex
            } else {
                lazyListState.firstVisibleItemIndex
            }
            val safeIndex = targetIndex.coerceIn(0, (albums.size - 1).coerceAtLeast(0))
            if (USE_NEW_FAST_GRID) {
                fastLazyListState.scrollToItem(safeIndex)
            } else {
                lazyGridState.scrollToItem(safeIndex)
            }
        } else {
            val targetIndex = if (USE_NEW_FAST_GRID) {
                val layoutInfo = fastLazyListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter =
                        (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2f
                        abs(itemCenter - viewportCenter)
                    }?.index ?: lazyListState.firstVisibleItemIndex
                } else {
                    lazyListState.firstVisibleItemIndex
                }
            } else {
                val layoutInfo = lazyGridState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter =
                        (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset.y + item.size.height / 2f
                        abs(itemCenter - viewportCenter)
                    }?.index ?: lazyListState.firstVisibleItemIndex
                } else {
                    lazyListState.firstVisibleItemIndex
                }
            }
            if (albums.isNotEmpty()) {
                lazyListState.scrollToItem(
                    targetIndex.coerceIn(
                        0,
                        (albums.size - 1).coerceAtLeast(0)
                    )
                )
            }
        }
    }

    // Index of the album currently centered on screen in slow scrolling grid
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2f
                    abs(itemCenter - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isScreenTouched = event.changes.any { it.pressed }
                    }
                }
            }
            .nestedScroll(nestedScrollConnection)
    ) {
        val targetedHeight = this@BoxWithConstraints.maxWidth * 0.90f
        val nonTargetedHeight = this@BoxWithConstraints.maxHeight * 0.072f
        val normalVerticalPadding = (this@BoxWithConstraints.maxHeight - targetedHeight) / 2
        val bottomContentPadding = normalVerticalPadding + contentPadding.calculateBottomPadding()

        // 1. Slow Scrolling 1-Column Layout (AlbumGridPunk)
        if (fastScrollProgress < 0.99f) {
            LazyColumn(
                state = lazyListState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState),
                contentPadding = PaddingValues(
                    top = normalVerticalPadding,
                    bottom = bottomContentPadding
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - fastScrollProgress
                        scaleX = lerp(1f, 0.92f, fastScrollProgress)
                        scaleY = lerp(1f, 0.92f, fastScrollProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                    val isTargeted = index == centerIndex

                    val normalItemHeight by remember {
                        derivedStateOf {
                            val layoutInfo = lazyListState.layoutInfo
                            val itemInfo =
                                layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                            if (itemInfo != null) {
                                val viewportCenter =
                                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val distancePx = abs(itemCenter - viewportCenter)
                                val maxDist =
                                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                                val fraction = (1f - (distancePx / maxDist)).coerceIn(0f, 1f)
                                nonTargetedHeight + (targetedHeight - nonTargetedHeight) * fraction
                            } else {
                                nonTargetedHeight
                            }
                        }
                    }

                    AlbumRowItem(
                        album = album,
                        isTargeted = isTargeted,
                        itemHeight = normalItemHeight,
                        fastScrollProgress = fastScrollProgress,
                        index = index,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }

        // 2. Fast Scrolling Layout (Messy Zigzag Card Stack OR Legacy 2-Column Grid)
        if (fastScrollProgress > 0.01f) {
            if (USE_NEW_FAST_GRID) {
                MessyFastScrollGrid(
                    albums = albums,
                    state = fastLazyListState,
                    contentPadding = contentPadding,
                    fastScrollProgress = fastScrollProgress,
                    onAlbumClick = onAlbumClick
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = lazyGridState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp + contentPadding.calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = fastScrollProgress
                            scaleX = lerp(0.88f, 1f, fastScrollProgress)
                            scaleY = lerp(0.88f, 1f, fastScrollProgress)
                        }
                ) {
                    itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                        FastGridAlbumItem(
                            album = album,
                            columnIndex = index % 2,
                            fastScrollProgress = fastScrollProgress,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }

        // ScrollBar Overlay
        SimpleScrollBar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(100f)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            listState = if (isFastModeActive && USE_NEW_FAST_GRID) fastLazyListState else if (!isFastModeActive) lazyListState else null,
            gridState = if (isFastModeActive && !USE_NEW_FAST_GRID) lazyGridState else null,
            onInteractingChanged = { interacting ->
                isScrollbarInteracting = interacting
            }
        )
    }
}

/**
 * Fast scrolling layout with vertically overlapping album artwork in a messy zigzag pattern.
 */
@Composable
fun MessyFastScrollGrid(
    albums: List<Album>,
    state: LazyListState,
    contentPadding: PaddingValues,
    fastScrollProgress: Float,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = 24.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy((-95).dp),
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = fastScrollProgress
                scaleX = lerp(0.88f, 1f, fastScrollProgress)
                scaleY = lerp(0.88f, 1f, fastScrollProgress)
            }
    ) {
        itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
            MessyAlbumCardItem(
                album = album,
                index = index,
                state = state,
                fastScrollProgress = fastScrollProgress,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

/**
 * Album card for the messy zigzag fast scroll layout.
 * Displays album art ONLY (no text labels by design) with staggered alignment, inner border frame,
 * dynamic shadows, and center-focused Z-index layering.
 */
@Composable
fun MessyAlbumCardItem(
    album: Album,
    index: Int,
    state: LazyListState,
    fastScrollProgress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isEven = index % 2 == 0
    val columnAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd
    val density = LocalDensity.current

    // Dynamic Z-index based on proximity to center of viewport (Center Focus)
    val dynamicZIndex by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (itemInfo != null) {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                val maxDist = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                val fraction = (1f - (abs(itemCenter - viewportCenter) / maxDist)).coerceIn(0f, 1f)
                fraction * 10f
            } else 0f
        }
    }

    val dynamicElevation by remember {
        derivedStateOf {
            4.dp + 8.dp * (dynamicZIndex / 10f)
        }
    }

    val xSlideDp = if (isEven) {
        lerp((-32).dp, 0.dp, fastScrollProgress)
    } else {
        lerp(32.dp, 0.dp, fastScrollProgress)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(dynamicZIndex)
            .graphicsLayer {
                translationX = xSlideDp.toPx()
                val scale = lerp(0.90f, 1f, fastScrollProgress)
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = columnAlignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .aspectRatio(1f)
                .graphicsLayer {
                    shadowElevation = with(density) { dynamicElevation.toPx() }
                    shape = RectangleShape
                    clip = false
                    spotShadowColor = Color.Black.copy(alpha = 0.65f)
                    ambientShadowColor = Color.Black.copy(alpha = 0.45f)
                }
                .clip(RectangleShape)
                .clickable { onClick() },
            shape = RectangleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = dynamicElevation),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RectangleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (album.artworkUri != null) {
                    val albumPlaceholder = rememberVectorPainter(Icons.Default.Album)
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.name,
                        error = albumPlaceholder,
                        placeholder = albumPlaceholder,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Album title overlay on top left corner with black background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RectangleShape)
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
fun FastGridAlbumItem(
    album: Album,
    modifier: Modifier = Modifier,
    columnIndex: Int = 0,
    fastScrollProgress: Float = 1f,
    onClick: () -> Unit
) {
    val displayArtist = remember(album.artist) {
        album.artist.splitArtistsByDelimiters().joinArtistsForDisplay()
    }

    val xSlideDp = if (columnIndex == 0) {
        lerp(24.dp, 0.dp, fastScrollProgress)
    } else {
        lerp((-24).dp, 0.dp, fastScrollProgress)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = xSlideDp.toPx()
                val scale = lerp(0.92f, 1f, fastScrollProgress)
                scaleX = scale
                scaleY = scale
            }
            .clip(RectangleShape)
            .clickable { onClick() },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RectangleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.artworkUri != null) {
                    val albumPlaceholder = rememberVectorPainter(Icons.Default.Album)
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.name,
                        error = albumPlaceholder,
                        placeholder = albumPlaceholder,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = displayArtist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumRowItem(
    album: Album,
    isTargeted: Boolean,
    itemHeight: Dp,
    fastScrollProgress: Float,
    modifier: Modifier = Modifier,
    index: Int = 0,
    onClick: () -> Unit
) {
    val displayArtist = remember(album.artist) {
        album.artist.splitArtistsByDelimiters().joinArtistsForDisplay()
    }

    // Step 1: Vertical Y-motion (1.0 -> 0.5)
    val phase1Progress = ((fastScrollProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)

    // Step 2: Horizontal X-slide & Artwork Expansion (0.5 -> 0.0)
    val phase2Progress = (fastScrollProgress / 0.5f).coerceIn(0f, 1f)

    val cardShape = RectangleShape
    val density = LocalDensity.current

    val columnSign = if (index % 2 == 0) -1f else 1f
    val yOffsetDp = if (index % 2 == 0) (-16).dp else 16.dp

    Card(
        modifier = modifier
            .fillMaxWidth(lerp(0.90f, 0.46f, phase2Progress))
            .height(itemHeight)
            .graphicsLayer {
                val yOffsetPx = with(density) { yOffsetDp.toPx() }
                val xOffsetPx = with(density) { 80.dp.toPx() } * columnSign
                translationY = lerp(0f, yOffsetPx, phase1Progress)
                translationX = lerp(0f, xOffsetPx, phase2Progress)
            }
            .clip(cardShape)
            .clickable { onClick() },
        shape = cardShape,
        border = null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = lerp(0.35f, 0.60f, phase2Progress)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork Container
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (phase2Progress > 0.01f) {
                            val artWidth = lerp(itemHeight, 52.dp, phase2Progress)
                            Modifier.width(artWidth)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .clip(RectangleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (album.artworkUri != null) {
                    val albumPlaceholder = rememberVectorPainter(Icons.Default.Album)
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.name,
                        error = albumPlaceholder,
                        placeholder = albumPlaceholder,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTargeted) 56.dp else 28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Normal Mode Overlays
                if (phase2Progress < 0.99f) {
                    val overlayAlpha = 1f - phase2Progress
                    if (isTargeted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .graphicsLayer { alpha = overlayAlpha }
                                .clip(RectangleShape)
                                .background(Color.Black.copy(alpha = 0.82f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = overlayAlpha }
                                .background(Color.Black.copy(alpha = 0.38f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Right Text Column when transitioning
            if (phase2Progress > 0.01f) {
                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = phase2Progress },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
fun MessyFastScrollGridPreview() {
    MaterialTheme {
        val sampleAlbums = listOf(
            Album(
                id = "1",
                name = "Abbey Road",
                artist = "The Beatles",
                year = 1969,
                artworkUri = null,
                trackCount = 17
            ),
            Album(
                id = "2",
                name = "Back in Black",
                artist = "AC/DC",
                year = 1980,
                artworkUri = null,
                trackCount = 10
            ),
            Album(
                id = "3",
                name = "Dark Side of the Moon",
                artist = "Pink Floyd",
                year = 1973,
                artworkUri = null,
                trackCount = 10
            ),
            Album(
                id = "4",
                name = "Hotel California",
                artist = "Eagles",
                year = 1976,
                artworkUri = null,
                trackCount = 9
            )
        )
        MessyFastScrollGrid(
            albums = sampleAlbums,
            state = rememberLazyListState(),
            contentPadding = PaddingValues(0.dp),
            fastScrollProgress = 1f,
            onAlbumClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
fun AlbumGridPunkPreview() {
    MaterialTheme {
        val sampleAlbums = listOf(
            Album(
                id = "1",
                name = "Abbey Road",
                artist = "The Beatles",
                year = 1969,
                artworkUri = null,
                trackCount = 17
            ),
            Album(
                id = "2",
                name = "Back in Black",
                artist = "AC/DC",
                year = 1980,
                artworkUri = null,
                trackCount = 10
            ),
            Album(
                id = "3",
                name = "Dark Side of the Moon",
                artist = "Pink Floyd",
                year = 1973,
                artworkUri = null,
                trackCount = 10
            ),
            Album(
                id = "4",
                name = "Hotel California",
                artist = "Eagles",
                year = 1976,
                artworkUri = null,
                trackCount = 9
            ),
            Album(
                id = "5",
                name = "Kind of Blue",
                artist = "Miles Davis",
                year = 1959,
                artworkUri = null,
                trackCount = 5
            ),
            Album(
                id = "6",
                name = "Rumours",
                artist = "Fleetwood Mac",
                year = 1977,
                artworkUri = null,
                trackCount = 11
            ),
            Album(
                id = "7",
                name = "Thriller",
                artist = "Michael Jackson",
                year = 1982,
                artworkUri = null,
                trackCount = 9
            ),
            Album(
                id = "8",
                name = "Wall, The",
                artist = "Pink Floyd",
                year = 1979,
                artworkUri = null,
                trackCount = 26
            )
        )
        AlbumGridPunk(
            albums = sampleAlbums,
            onAlbumClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FastGridAlbumItemPreview() {
    MaterialTheme {
        Box(modifier = Modifier
            .width(180.dp)
            .padding(8.dp)) {
            FastGridAlbumItem(
                album = Album(
                    id = "1",
                    name = "Abbey Road",
                    artist = "The Beatles",
                    year = 1969,
                    artworkUri = null,
                    trackCount = 17
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumRowItemPreview() {
    MaterialTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            AlbumRowItem(
                album = Album(
                    id = "1",
                    name = "Abbey Road",
                    artist = "The Beatles",
                    year = 1969,
                    artworkUri = null,
                    trackCount = 17
                ),
                isTargeted = true,
                itemHeight = 260.dp,
                fastScrollProgress = 0f,
                onClick = {}
            )
        }
    }
}
