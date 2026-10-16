package io.github.vinaygaykar.mp1.ui.library

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
            color = MaterialTheme.colorScheme.surface,
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
            containerColor = MaterialTheme.colorScheme.surface,
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
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIndices.isNotEmpty()
    var showQueueMultiSelectionSheet by remember { mutableStateOf(false) }

    fun toggleSelection(index: Int) {
        selectedIndices = if (selectedIndices.contains(index)) {
            selectedIndices - index
        } else {
            selectedIndices + index
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedIndices = emptySet()
    }

    LaunchedEffect(state.queue.size) {
        selectedIndices = selectedIndices.filter { it in state.queue.indices }.toSet()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (state.queue.isEmpty()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            var hasScrolledToInitialPosition by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            val extraOffsetPx = remember(density) { with(density) { 12.dp.roundToPx() } }

            // Scroll to current playing song ONCE when sheet opens
            LaunchedEffect(stickyHeaderHeightPx, state.currentIndex, state.queue.size) {
                if (!hasScrolledToInitialPosition && state.currentIndex in state.queue.indices) {
                    if (stickyHeaderHeightPx > 0) {
                        val scrollOffset = -(stickyHeaderHeightPx + extraOffsetPx)
                        lazyListState.scrollToItem(
                            index = state.currentIndex + 1,
                            scrollOffset = scrollOffset
                        )
                        hasScrolledToInitialPosition = true
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Header: Action Row & Animated "Playing Queue" Title (Sticky Header)
                    stickyHeader(key = "header_actions") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
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
                                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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

                                if (isSelectionMode) {
                                    SelectionActionRow(
                                        selectedCount = selectedIndices.size,
                                        onSelectAll = { selectedIndices = state.queue.indices.toSet() },
                                        onDeselect = { selectedIndices = emptySet() },
                                        onOptionsClick = { showQueueMultiSelectionSheet = true },
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            onSelect = {
                                if (isSelectionMode) {
                                    toggleSelection(index)
                                } else {
                                    actions.onSkipToQueueItem(index)
                                }
                            },
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
                            },
                            isSelected = selectedIndices.contains(index),
                            isSelectionMode = isSelectionMode,
                            onLongClick = { toggleSelection(index) }
                        )
                    }
                }

                if (state.queue.size > 50) {
                    SimpleScrollBar(
                        listState = lazyListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
                            .zIndex(100f)
                    )
                }
            }

            val selectedItems = remember(state.queue, selectedIndices) {
                selectedIndices.mapNotNull { state.queue.getOrNull(it) }
            }

            if (showQueueMultiSelectionSheet) {
                QueueMultiSelectionBottomSheet(
                    selectedItems = selectedItems,
                    onDismiss = { showQueueMultiSelectionSheet = false },
                    onRemoveFromQueue = {
                        actions.removeIndicesFromQueue(selectedIndices)
                        selectedIndices = emptySet()
                    },
                    onRemoveOthersFromQueue = {
                        actions.removeOthersFromQueue(selectedIndices)
                        selectedIndices = emptySet()
                    },
                    onPlayNext = {
                        actions.playNextInQueue(selectedIndices)
                        selectedIndices = emptySet()
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@OptIn(ExperimentalFoundationApi::class)
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
    onDragEnd: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val view = LocalView.current

    val itemShape = if (isCurrent || isSelected || isBeingDragged) CircleShape else RoundedCornerShape(12.dp)

    val containerColor by animateColorAsState(
        targetValue = when {
            isBeingDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        label = "queue_item_bg"
    )

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

    ListItem(
        headlineContent = {
            Text(
                text = if (rawTitle.isBlank()) "Unknown Track" else rawTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                    isCurrent || isBeingDragged -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = displayArtist,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    isCurrent || isBeingDragged -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drag Rearrange Handle on left
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .then(
                            if (!isSelectionMode) {
                                Modifier.pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { currentOnDragStart() },
                                        onDragEnd = { currentOnDragEnd() },
                                        onDragCancel = { currentOnDragEnd() },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            currentOnDrag(dragAmount)
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragIndicator,
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
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                    AsyncImage(
                        model = item.mediaMetadata.artworkUri,
                        contentDescription = null,
                        error = musicPlaceholder,
                        placeholder = musicPlaceholder,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            if (isSelectionMode) {
                IconButton(
                    onClick = onLongClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                FilledTonalIconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isCurrent || isBeingDragged) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (isCurrent || isBeingDragged) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove from Queue",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .zIndex(if (isBeingDragged) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (isBeingDragged) 1.03f else 1f
                scaleY = if (isBeingDragged) 1.03f else 1f
            }
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
            .background(containerColor)
            .combinedClickable(
                enabled = !isBeingDragged,
                onClick = {
                    if (isSelectionMode) {
                        onLongClick()
                    } else {
                        onSelect()
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueMultiSelectionBottomSheet(
    selectedItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    onRemoveOthersFromQueue: () -> Unit,
    onPlayNext: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // First Row: Overlapping circular artworks on left, selected count on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverlappingMediaItemArtworksRow(items = selectedItems)

                Spacer(modifier = Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${selectedItems.size}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (selectedItems.size == 1) "Song Selected" else "Songs Selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Row 1: Remove and Remove others
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remove button
                FilledTonalButton(
                    onClick = {
                        onRemoveFromQueue()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Remove others button
                FilledTonalButton(
                    onClick = {
                        onRemoveOthersFromQueue()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClearAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove others",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Play next (Bigger and square shaped with rounded corners)
            Button(
                onClick = {
                    onPlayNext()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Play next",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OverlappingMediaItemArtworksRow(
    items: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(items) { items.take(5) }
    val overlapOffset = 32.dp
    val avatarSize = 56.dp

    Box(
        modifier = modifier
            .width(avatarSize + overlapOffset * (displayItems.size - 1).coerceAtLeast(0))
            .height(avatarSize),
        contentAlignment = Alignment.CenterStart
    ) {
        displayItems.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .offset(x = overlapOffset * index)
                    .size(avatarSize)
                    .zIndex((displayItems.size - index).toFloat())
                    .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val artworkUri = item.mediaMetadata.artworkUri
                if (artworkUri != null) {
                    val musicPlaceholder = rememberVectorPainter(Icons.Default.MusicNote)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        error = musicPlaceholder,
                        placeholder = musicPlaceholder,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
