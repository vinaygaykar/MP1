package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    state: PlayerUiState,
    actions: PlayerActions,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxSize()
        ) {
            QueueBottomSheetContent(state = state, actions = actions)
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            modifier = modifier.fillMaxHeight()
        ) {
            QueueBottomSheetContent(state = state, actions = actions)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheetContent(
    state: PlayerUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (state.queue.isEmpty()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Playing Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items in queue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val lazyListState = rememberLazyListState()
            var draggedKey by remember { mutableStateOf<String?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var stickyHeaderHeightPx by remember { mutableIntStateOf(0) }
            val coroutineScope = rememberCoroutineScope()

            var isPlayingQueueHeaderVisible by remember { mutableStateOf(true) }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        if (delta < -12f) {
                            isPlayingQueueHeaderVisible = false
                        } else if (delta > 12f) {
                            isPlayingQueueHeaderVisible = true
                        }
                        return Offset.Zero
                    }
                }
            }

            val isAtTop by remember {
                derivedStateOf {
                    lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                }
            }

            val showPlayingQueueHeader = isPlayingQueueHeaderVisible || isAtTop

            // Scroll to current playing song when sheet opens or current index changes
            LaunchedEffect(state.currentIndex) {
                if (state.currentIndex in state.queue.indices) {
                    if (state.currentIndex == 0) {
                        lazyListState.scrollToItem(0, 0)
                    } else {
                        val scrollOffset = if (stickyHeaderHeightPx > 0) -stickyHeaderHeightPx else -150
                        lazyListState.scrollToItem(
                            index = state.currentIndex + 1,
                            scrollOffset = scrollOffset
                        )
                    }
                }
            }

            val itemKeys = remember(state.queue) {
                val counts = mutableMapOf<Int, Int>()
                state.queue.map { item ->
                    val identity = System.identityHashCode(item)
                    val count = counts.getOrDefault(identity, 0)
                    counts[identity] = count + 1
                    val baseId = item.mediaId.ifEmpty { item.mediaMetadata.title?.toString() ?: "item" }
                    if (count == 0) "${baseId}_$identity" else "${baseId}_${identity}_$count"
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Action Row & Animated "Playing Queue" Title (Sticky Header)
                    stickyHeader(key = "header_actions") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { layoutCoordinates ->
                                    stickyHeaderHeightPx = layoutCoordinates.size.height
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = showPlayingQueueHeader,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Playing Queue",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Text(
                                                    text = "${state.queue.size}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val isShuffleEnabled = state.queue.size > 1
                                    val shuffleContainerColor by animateColorAsState(
                                        targetValue = if (state.isShuffleOn) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLowest
                                        },
                                        label = "shuffle_btn_bg"
                                    )
                                    val shuffleContentColor by animateColorAsState(
                                        targetValue = if (state.isShuffleOn) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        label = "shuffle_btn_content"
                                    )

                                    // Clear All Button (Left)
                                    OutlinedButton(
                                        onClick = { actions.clearQueue() },
                                        enabled = state.queue.size > 1,
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ClearAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Shuffle Button (Right)
                                    FilledTonalButton(
                                        onClick = { actions.onShuffleToggle() },
                                        enabled = isShuffleEnabled,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = shuffleContainerColor,
                                            contentColor = shuffleContentColor,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (state.isShuffleOn) "Shuffle On" else "Shuffle Off",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Queue Songs
                    itemsIndexed(
                        items = state.queue,
                        key = { index, _ -> itemKeys.getOrElse(index) { "item_$index" } }
                    ) { index, item ->
                        val isCurrent = index == state.currentIndex
                        val key = itemKeys.getOrElse(index) { "item_$index" }
                        val isBeingDragged = draggedKey == key

                        QueueItemRow(
                            item = item,
                            isCurrent = isCurrent,
                            isBeingDragged = isBeingDragged,
                            dragOffsetY = if (isBeingDragged) dragOffsetY else 0f,
                            onSelect = { actions.onSkipToQueueItem(index) },
                            onRemove = { actions.removeFromQueue(index) },
                            onDragStart = {
                                draggedKey = key
                                dragOffsetY = 0f
                            },
                            onDrag = { dragAmount ->
                                val currentKey = draggedKey ?: return@QueueItemRow
                                dragOffsetY += dragAmount

                                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                                val listIndex = itemKeys.indexOf(currentKey)
                                if (listIndex == -1) return@QueueItemRow
                                val lazyListIndex = listIndex + 1

                                val draggedItemInfo = visibleItems.firstOrNull { it.index == lazyListIndex }
                                if (draggedItemInfo != null) {
                                    val draggedItemTop = draggedItemInfo.offset + dragOffsetY
                                    val draggedItemCenter = draggedItemTop + draggedItemInfo.size / 2f

                                    val targetItem = visibleItems.firstOrNull { itemInfo ->
                                        itemInfo.index >= 1 &&
                                                itemInfo.index != lazyListIndex &&
                                                draggedItemCenter >= itemInfo.offset &&
                                                draggedItemCenter <= (itemInfo.offset + itemInfo.size)
                                    }

                                    if (targetItem != null) {
                                        val targetQueueIndex = targetItem.index - 1
                                        actions.moveQueueItem(listIndex, targetQueueIndex)
                                        dragOffsetY += (draggedItemInfo.offset - targetItem.offset)
                                    }

                                    val viewportHeight = lazyListState.layoutInfo.viewportSize.height
                                    if (draggedItemTop < 100f) {
                                        coroutineScope.launch { lazyListState.scrollBy(-20f) }
                                    } else if (draggedItemTop > viewportHeight - 100f) {
                                        coroutineScope.launch { lazyListState.scrollBy(20f) }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedKey = null
                                dragOffsetY = 0f
                            }
                        )
                    }
                }

                if (state.queue.size > 50) {
                    SimpleScrollBar(
                        listState = lazyListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp, top = 8.dp, bottom = 8.dp)
                            .zIndex(100f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun QueueItemRow(
    item: MediaItem,
    isCurrent: Boolean,
    isBeingDragged: Boolean,
    dragOffsetY: Float,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val itemShape = if (isCurrent) CircleShape else RoundedCornerShape(20.dp)

    val containerColor by animateColorAsState(
        targetValue = when {
            isBeingDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "queue_item_bg"
    )

    val contentColor = if (isCurrent || isBeingDragged) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val elevation by animateDpAsState(
        targetValue = if (isBeingDragged) 8.dp else (if (isCurrent) 4.dp else 1.dp),
        label = "queue_item_elevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (isBeingDragged) 1.03f else 1f
                scaleY = if (isBeingDragged) 1.03f else 1f
            }
            .clip(itemShape)
            .clickable(enabled = !isBeingDragged, onClick = onSelect),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        shape = itemShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag Rearrange Handle (Left of Album Art)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { currentOnDragStart() },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDrag(dragAmount)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = if (isCurrent || isBeingDragged) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Artwork Thumbnail
            AsyncImage(
                model = item.mediaMetadata.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )

            // Title & Artist
            val rawArtist = item.mediaMetadata.artist?.toString() ?: ""
            val rawTitle = item.mediaMetadata.title?.toString() ?: ""
            val displayArtist = remember(rawArtist, rawTitle) {
                val artistList = collectArtistNames(rawArtist, rawTitle)
                if (artistList.isNotEmpty()) {
                    artistList.joinArtistsForDisplay()
                } else {
                    rawArtist.ifBlank { "Unknown Artist" }
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (rawTitle.isBlank()) "Unknown Track" else rawTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent || isBeingDragged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent || isBeingDragged) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Remove from Queue Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove from Queue",
                    tint = if (isCurrent || isBeingDragged) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private object PreviewPlayerActions : PlayerActions {
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

@Preview(showBackground = true)
@Composable
fun QueueBottomSheetPreview() {
    val sampleState = PlayerUiState(
        queue = persistentListOf(
            MediaItem.Builder()
                .setMediaId("1")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Bohemian Rhapsody")
                        .setArtist("Queen")
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId("2")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Hotel California")
                        .setArtist("Eagles")
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId("3")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Stairway to Heaven")
                        .setArtist("Led Zeppelin")
                        .build()
                )
                .build()
        ),
        currentIndex = 0,
        isShuffleOn = false
    )

    MusicPlayerTheme {
        QueueBottomSheet(
            state = sampleState,
            actions = PreviewPlayerActions,
            onDismissRequest = {}
        )
    }
}




