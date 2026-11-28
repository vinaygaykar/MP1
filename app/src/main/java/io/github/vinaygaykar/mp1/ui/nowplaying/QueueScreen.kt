package io.github.vinaygaykar.mp1.ui.nowplaying

import android.app.Activity
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.vinaygaykar.mp1.playback.QueueControls
import io.github.vinaygaykar.mp1.playback.QueueUiState
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar
import io.github.vinaygaykar.mp1.ui.library.SelectionActionRow
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queueState: QueueUiState,
    actions: QueueControls,
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxSize()
        ) {
            QueueBottomSheetContent(
                queueState = queueState,
                actions = actions,
                isShuffleOn = isShuffleOn,
                onShuffleToggle = onShuffleToggle,
                onDismiss = onDismissRequest
            )
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            sheetMaxWidth = Dp.Unspecified,
            modifier = modifier.fillMaxHeight()
        ) {
            QueueBottomSheetContent(
                queueState = queueState,
                actions = actions,
                isShuffleOn = isShuffleOn,
                onShuffleToggle = onShuffleToggle,
                onDismiss = onDismissRequest
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun QueueBottomSheetContent(
    queueState: QueueUiState,
    actions: QueueControls,
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    initialSelectedIndices: Set<Int> = emptySet()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }

    val widthClass = windowSizeClass?.widthSizeClass
    val heightClass = windowSizeClass?.heightSizeClass

    val isWideWindow = if (widthClass != null) {
        widthClass != WindowWidthSizeClass.Compact
    } else {
        val configuration = LocalConfiguration.current
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val isCompactHeight = if (heightClass != null) {
        heightClass == WindowHeightSizeClass.Compact
    } else {
        val configuration = LocalConfiguration.current
        configuration.screenHeightDp < 500
    }

    when {
        isWideWindow -> {
            QueueLandscapeContent(
                queueState = queueState,
                actions = actions,
                isShuffleOn = isShuffleOn,
                onShuffleToggle = onShuffleToggle,
                modifier = modifier,
                onDismiss = onDismiss,
                initialSelectedIndices = initialSelectedIndices
            )
        }

        isCompactHeight -> {
            QueueCompactContent(
                queueState = queueState,
                actions = actions,
                isShuffleOn = isShuffleOn,
                onShuffleToggle = onShuffleToggle,
                modifier = modifier,
                onDismiss = onDismiss,
                initialSelectedIndices = initialSelectedIndices
            )
        }

        else -> {
            QueuePortraitContent(
                queueState = queueState,
                actions = actions,
                isShuffleOn = isShuffleOn,
                onShuffleToggle = onShuffleToggle,
                modifier = modifier,
                onDismiss = onDismiss,
                initialSelectedIndices = initialSelectedIndices
            )
        }
    }
}

@Composable
private fun QueueHeaderTitle(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
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
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun DismissIconButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onDismiss,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Dismiss",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun QueueCompactContent(
    queueState: QueueUiState,
    actions: QueueControls,
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    initialSelectedIndices: Set<Int> = emptySet()
) {
    var selectedIndices by remember { mutableStateOf(initialSelectedIndices) }
    val isSelectionMode = selectedIndices.isNotEmpty()
    var showQueueMultiSelectionSheet by remember { mutableStateOf(false) }
    var showKebabOptionsMenuSheet by remember { mutableStateOf(false) }
    var isFloatingPillVisible by remember { mutableStateOf(true) }

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

    LaunchedEffect(queueState.items.size) {
        selectedIndices = selectedIndices.filter { it in queueState.items.indices }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            QueueItemList(
                queueState = queueState,
                actions = actions,
                selectedIndices = selectedIndices,
                onToggleSelection = ::toggleSelection,
                onTitleVisibilityChange = { isFloatingPillVisible = it },
                headerContent = {
                    QueueCompactHeader(
                        count = queueState.items.size,
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedIndices.size,
                        onSelectAll = { selectedIndices = queueState.items.indices.toSet() },
                        onDeselect = { selectedIndices = emptySet() },
                        onOptionsClick = { showQueueMultiSelectionSheet = true },
                        onDismiss = onDismiss
                    )
                }
            )
        }

        AnimatedVisibility(
            visible = isFloatingPillVisible && !showKebabOptionsMenuSheet && !showQueueMultiSelectionSheet,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QueueFloatingActionsPill(
                isShuffleOn = isShuffleOn,
                isShuffleEnabled = queueState.items.size > 1,
                onShuffleToggle = onShuffleToggle,
                onKebabMenuClick = { showKebabOptionsMenuSheet = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }

    QueueMultiSelectionSheetHost(
        showSheet = showQueueMultiSelectionSheet,
        selectedIndices = selectedIndices,
        items = queueState.items,
        actions = actions,
        onDismissSheet = { showQueueMultiSelectionSheet = false },
        onClearSelection = { selectedIndices = emptySet() }
    )

    QueueKebabOptionsMenuSheet(
        visible = showKebabOptionsMenuSheet,
        isShuffleOn = isShuffleOn,
        isShuffleEnabled = queueState.items.size > 1,
        isClearEnabled = queueState.items.isNotEmpty(),
        onShuffleToggle = onShuffleToggle,
        onClearQueue = { actions.clearQueue() },
        onDismiss = { showKebabOptionsMenuSheet = false }
    )

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@Composable
private fun QueueCompactHeader(
    count: Int,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselect: () -> Unit,
    onOptionsClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QueueHeaderTitle(count = count)
                DismissIconButton(onDismiss = onDismiss)
            }

            if (isSelectionMode) {
                SelectionActionRow(
                    selectedCount = selectedCount,
                    onSelectAll = onSelectAll,
                    onDeselect = onDeselect,
                    onOptionsClick = onOptionsClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QueueItemList(
    queueState: QueueUiState,
    actions: QueueControls,
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTitleVisibilityChange: ((Boolean) -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    topPadding: Dp = 0.dp
) {
    val headerOffset = if (headerContent != null) 1 else 0

    if (queueState.items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (headerContent != null) {
                    headerContent()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items in queue",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        val lazyListState = rememberLazyListState()
        var draggedKey by remember { mutableStateOf<String?>(null) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        var hasScrolledToInitialPosition by remember { mutableStateOf(false) }

        val nestedScrollConnection = remember(onTitleVisibilityChange) {
            if (onTitleVisibilityChange != null) {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y
                        if (draggedKey == null) {
                            if (delta < -8f) {
                                onTitleVisibilityChange(false)
                            } else if (delta > 8f) {
                                onTitleVisibilityChange(true)
                            }
                        }
                        return Offset.Zero
                    }
                }
            } else null
        }

        LaunchedEffect(lazyListState, onTitleVisibilityChange) {
            if (onTitleVisibilityChange != null) {
                snapshotFlow {
                    lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                }.collect { isAtTop ->
                    if (isAtTop) {
                        onTitleVisibilityChange(true)
                    }
                }
            }
        }

        LaunchedEffect(queueState.currentIndex, queueState.items.size) {
            if (!hasScrolledToInitialPosition && queueState.currentIndex in queueState.items.indices) {
                val targetIndex =
                    if (queueState.currentIndex == 0) 0 else queueState.currentIndex + headerOffset
                lazyListState.scrollToItem(index = targetIndex)
                hasScrolledToInitialPosition = true
            }
        }

        val itemKeys = remember(queueState.items) {
            val counts = mutableMapOf<Int, Int>()
            queueState.items.map { item ->
                val identity = System.identityHashCode(item)
                val count = counts.getOrDefault(identity, 0)
                counts[identity] = count + 1
                val baseId = item.mediaId.ifEmpty { item.mediaMetadata.title?.toString() ?: "item" }
                if (count == 0) "${baseId}_$identity" else "${baseId}_${identity}_$count"
            }
        }

        val isSelectionMode = selectedIndices.isNotEmpty()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(modifier)
                .then(
                    if (nestedScrollConnection != null) {
                        Modifier.nestedScroll(nestedScrollConnection)
                    } else Modifier
                )
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (headerContent != null) 0.dp else topPadding,
                    bottom = 88.dp
                )
            ) {
                if (headerContent != null) {
                    item(key = "queue_header_content") {
                        headerContent()
                    }
                }

                itemsIndexed(
                    items = queueState.items,
                    key = { index, _ -> itemKeys.getOrElse(index) { "item_$index" } }
                ) { index, item ->
                    val itemKey = itemKeys.getOrElse(index) { "item_$index" }
                    val isCurrent = index == queueState.currentIndex
                    val isSelected = selectedIndices.contains(index)
                    val isBeingDragged = draggedKey == itemKey

                    QueueItemRow(
                        item = item,
                        isCurrent = isCurrent,
                        isBeingDragged = isBeingDragged,
                        dragOffsetY = if (isBeingDragged) dragOffsetY else 0f,
                        onSelect = {
                            if (isSelectionMode) {
                                onToggleSelection(index)
                            } else {
                                actions.onSkipToQueueItem(index)
                            }
                        },
                        onRemove = {
                            actions.removeFromQueue(index)
                        },
                        onDragStart = {
                            draggedKey = itemKey
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY ->
                            dragOffsetY += deltaY
                            val layoutInfo = lazyListState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val currentItemInfo = visibleItems.find { it.key == draggedKey }

                            if (currentItemInfo != null) {
                                val threshold = currentItemInfo.size / 2f
                                val currentIndex = visibleItems.indexOf(currentItemInfo)

                                if (dragOffsetY > threshold && currentIndex < visibleItems.size - 1) {
                                    val targetItem = visibleItems[currentIndex + 1]
                                    val fromIdx = currentItemInfo.index - headerOffset
                                    val toIdx = targetItem.index - headerOffset
                                    if (fromIdx in queueState.items.indices && toIdx in queueState.items.indices) {
                                        actions.moveQueueItem(fromIdx, toIdx)
                                        dragOffsetY -= currentItemInfo.size
                                    }
                                } else if (dragOffsetY < -threshold && currentIndex > 0) {
                                    val targetItem = visibleItems[currentIndex - 1]
                                    val fromIdx = currentItemInfo.index - headerOffset
                                    val toIdx = targetItem.index - headerOffset
                                    if (fromIdx in queueState.items.indices && toIdx in queueState.items.indices) {
                                        actions.moveQueueItem(fromIdx, toIdx)
                                        dragOffsetY += currentItemInfo.size
                                    }
                                }

                                val viewportHeight =
                                    layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                val topBounds = currentItemInfo.offset + dragOffsetY
                                val bottomBounds = topBounds + currentItemInfo.size

                                if (bottomBounds > viewportHeight - 50) {
                                    coroutineScope.launch { lazyListState.scrollBy(15f) }
                                } else if (topBounds < 50) {
                                    coroutineScope.launch { lazyListState.scrollBy(-15f) }
                                }
                            }
                        },
                        onDragEnd = {
                            draggedKey = null
                            dragOffsetY = 0f
                        },
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = { onToggleSelection(index) }
                    )
                }
            }

            SimpleScrollBar(
                listState = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd),
                paddingEnd = 8.dp
            )
        }
    }
}

@Composable
private fun QueueMultiSelectionSheetHost(
    showSheet: Boolean,
    selectedIndices: Set<Int>,
    items: List<MediaItem>,
    actions: QueueControls,
    onDismissSheet: () -> Unit,
    onClearSelection: () -> Unit
) {
    if (showSheet) {
        val selectedItems = remember(selectedIndices, items) {
            selectedIndices.mapNotNull { items.getOrNull(it) }
        }

        QueueMultiSelectionBottomSheet(
            selectedItems = selectedItems,
            onDismiss = onDismissSheet,
            onRemoveFromQueue = {
                actions.removeIndicesFromQueue(selectedIndices)
                onClearSelection()
            },
            onRemoveOthersFromQueue = {
                actions.removeOthersFromQueue(selectedIndices)
                onClearSelection()
            },
            onPlayNext = {
                actions.playNextInQueue(selectedIndices)
                onClearSelection()
            }
        )
    }
}

@Composable
private fun QueuePortraitContent(
    queueState: QueueUiState,
    actions: QueueControls,
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    initialSelectedIndices: Set<Int> = emptySet()
) {
    var selectedIndices by remember { mutableStateOf(initialSelectedIndices) }
    val isSelectionMode = selectedIndices.isNotEmpty()
    var showQueueMultiSelectionSheet by remember { mutableStateOf(false) }
    var showKebabOptionsMenuSheet by remember { mutableStateOf(false) }
    var isTitleVisible by remember { mutableStateOf(true) }
    var isFloatingPillVisible by remember { mutableStateOf(true) }

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

    LaunchedEffect(queueState.items.size) {
        selectedIndices = selectedIndices.filter { it in queueState.items.indices }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topBarPadding = topInset + (if (isSelectionMode) 116.dp else 72.dp)

        // QueueItemList fills the whole screen so scrolling content passes underneath status bar
        QueueItemList(
            queueState = queueState,
            actions = actions,
            selectedIndices = selectedIndices,
            onToggleSelection = ::toggleSelection,
            onTitleVisibilityChange = { visible ->
                isTitleVisible = visible
                isFloatingPillVisible = visible
            },
            modifier = Modifier.fillMaxSize(),
            topPadding = topBarPadding
        )

        // Top Header Surface floating over QueueItemList
        AnimatedVisibility(
            visible = isTitleVisible || isSelectionMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 6.dp)
                ) {
                    if (isTitleVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QueueHeaderTitle(count = queueState.items.size)
                            DismissIconButton(onDismiss = onDismiss)
                        }
                    }

                    // Selection Action Row (Static)
                    if (isSelectionMode) {
                        SelectionActionRow(
                            selectedCount = selectedIndices.size,
                            onSelectAll = { selectedIndices = queueState.items.indices.toSet() },
                            onDeselect = { selectedIndices = emptySet() },
                            onOptionsClick = { showQueueMultiSelectionSheet = true }
                        )
                    }
                }
            }
        }

        // Floating Pill with 2 circular buttons in bottom center
        AnimatedVisibility(
            visible = isFloatingPillVisible && !showKebabOptionsMenuSheet && !showQueueMultiSelectionSheet,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QueueFloatingActionsPill(
                isShuffleOn = isShuffleOn,
                isShuffleEnabled = queueState.items.size > 1,
                onShuffleToggle = onShuffleToggle,
                onKebabMenuClick = { showKebabOptionsMenuSheet = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }

    QueueMultiSelectionSheetHost(
        showSheet = showQueueMultiSelectionSheet,
        selectedIndices = selectedIndices,
        items = queueState.items,
        actions = actions,
        onDismissSheet = { showQueueMultiSelectionSheet = false },
        onClearSelection = { selectedIndices = emptySet() }
    )

    QueueKebabOptionsMenuSheet(
        visible = showKebabOptionsMenuSheet,
        isShuffleOn = isShuffleOn,
        isShuffleEnabled = queueState.items.size > 1,
        isClearEnabled = queueState.items.isNotEmpty(),
        onShuffleToggle = onShuffleToggle,
        onClearQueue = { actions.clearQueue() },
        onDismiss = { showKebabOptionsMenuSheet = false }
    )

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@Composable
private fun QueueLandscapeContent(
    queueState: QueueUiState,
    actions: QueueControls,
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    initialSelectedIndices: Set<Int> = emptySet()
) {
    var selectedIndices by remember { mutableStateOf(initialSelectedIndices) }
    val isSelectionMode = selectedIndices.isNotEmpty()
    var showQueueMultiSelectionSheet by remember { mutableStateOf(false) }
    var showKebabOptionsMenuSheet by remember { mutableStateOf(false) }
    var isTitleVisible by remember { mutableStateOf(true) }
    var isFloatingPillVisible by remember { mutableStateOf(true) }

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

    LaunchedEffect(queueState.items.size) {
        selectedIndices = selectedIndices.filter { it in queueState.items.indices }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Surface (Playing Queue Title Row)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedVisibility(
                        visible = isTitleVisible || isSelectionMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .windowInsetsPadding(
                                    WindowInsets.displayCutout.only(
                                        WindowInsetsSides.Horizontal
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QueueHeaderTitle(count = queueState.items.size)
                                DismissIconButton(onDismiss = onDismiss)
                            }

                            if (isSelectionMode) {
                                SelectionActionRow(
                                    selectedCount = selectedIndices.size,
                                    onSelectAll = {
                                        selectedIndices = queueState.items.indices.toSet()
                                    },
                                    onDeselect = { selectedIndices = emptySet() },
                                    onOptionsClick = { showQueueMultiSelectionSheet = true }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            QueueItemList(
                queueState = queueState,
                actions = actions,
                selectedIndices = selectedIndices,
                onToggleSelection = ::toggleSelection,
                onTitleVisibilityChange = { visible ->
                    isTitleVisible = visible
                    isFloatingPillVisible = visible
                },
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.displayCutout.only(
                        WindowInsetsSides.Horizontal
                    )
                )
            )
        }

        // Floating Pill with 2 circular buttons in bottom right (vertical layout with background)
        AnimatedVisibility(
            visible = isFloatingPillVisible && !showKebabOptionsMenuSheet && !showQueueMultiSelectionSheet,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            QueueFloatingActionsPill(
                isShuffleOn = isShuffleOn,
                isShuffleEnabled = queueState.items.size > 1,
                onShuffleToggle = onShuffleToggle,
                onKebabMenuClick = { showKebabOptionsMenuSheet = true },
                isVertical = true,
                showPillBackground = true,
                modifier = Modifier
                    .navigationBarsPadding()
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(
                            WindowInsetsSides.End
                        )
                    )
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }

    QueueMultiSelectionSheetHost(
        showSheet = showQueueMultiSelectionSheet,
        selectedIndices = selectedIndices,
        items = queueState.items,
        actions = actions,
        onDismissSheet = { showQueueMultiSelectionSheet = false },
        onClearSelection = { selectedIndices = emptySet() }
    )

    QueueKebabOptionsMenuSheet(
        visible = showKebabOptionsMenuSheet,
        isShuffleOn = isShuffleOn,
        isShuffleEnabled = queueState.items.size > 1,
        isClearEnabled = queueState.items.isNotEmpty(),
        onShuffleToggle = onShuffleToggle,
        onClearQueue = { actions.clearQueue() },
        onDismiss = { showKebabOptionsMenuSheet = false }
    )

    Spacer(modifier = Modifier.navigationBarsPadding())
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

    val itemShape =
        if (isCurrent || isSelected || isBeingDragged) CircleShape else RoundedCornerShape(12.dp)

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
                    isCurrent || isBeingDragged -> MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.8f
                    )

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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.mediaMetadata.artworkUri)
                            .crossfade(true)
                            .build(),
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

@Composable
private fun QueueFloatingActionsPill(
    isShuffleOn: Boolean,
    isShuffleEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onKebabMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    showPillBackground: Boolean = true
) {
    val shuffleContainerColor by animateColorAsState(
        targetValue = if (isShuffleOn) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "floating_shuffle_bg"
    )
    val shuffleContentColor by animateColorAsState(
        targetValue = if (isShuffleOn) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "floating_shuffle_content"
    )

    val buttonsContent = @Composable {
        if (isVertical) {
            Column(
                modifier = Modifier.padding(
                    vertical = if (showPillBackground) 8.dp else 0.dp,
                    horizontal = if (showPillBackground) 6.dp else 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Button 1: Shuffle toggle
                FilledTonalIconButton(
                    onClick = onShuffleToggle,
                    enabled = isShuffleEnabled,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = shuffleContainerColor,
                        contentColor = shuffleContentColor,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = 0.5f
                        ),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.38f
                        )
                    )
                ) {
                    Icon(
                        imageVector = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.SwapHoriz,
                        contentDescription = if (isShuffleOn) "Shuffle On" else "Shuffle Off",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Button 2: Kebab menu
                FilledTonalIconButton(
                    onClick = onKebabMenuClick,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Shuffle toggle
                FilledTonalIconButton(
                    onClick = onShuffleToggle,
                    enabled = isShuffleEnabled,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = shuffleContainerColor,
                        contentColor = shuffleContentColor,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = 0.5f
                        ),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.38f
                        )
                    )
                ) {
                    Icon(
                        imageVector = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.SwapHoriz,
                        contentDescription = if (isShuffleOn) "Shuffle On" else "Shuffle Off",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Button 2: Kebab menu
                FilledTonalIconButton(
                    onClick = onKebabMenuClick,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showPillBackground) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = modifier
        ) {
            buttonsContent()
        }
    } else {
        Box(modifier = modifier) {
            buttonsContent()
        }
    }
}


@Composable
fun QueueKebabOptionsMenuSheet(
    visible: Boolean,
    isShuffleOn: Boolean,
    isShuffleEnabled: Boolean,
    isClearEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    // Scrim: Fades in and out without translation
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Custom Overlay: Slides in/out from bottom of screen (portrait) or right of screen (landscape)
    AnimatedVisibility(
        visible = visible,
        enter = if (isLandscape) {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        } else {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        exit = if (isLandscape) {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        } else {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLandscape) Alignment.BottomEnd else Alignment.BottomCenter
        ) {
            QueueKebabOptionsMenuContent(
                isShuffleOn = isShuffleOn,
                isShuffleEnabled = isShuffleEnabled,
                isClearEnabled = isClearEnabled,
                onShuffleToggle = {
                    onShuffleToggle()
                    onDismiss()
                },
                onClearQueue = {
                    onClearQueue()
                    onDismiss()
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun QueueKebabOptionsMenuContent(
    isShuffleOn: Boolean,
    isShuffleEnabled: Boolean,
    isClearEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val baseColor = MaterialTheme.colorScheme.primaryContainer

    val gradientBackground = remember(baseColor, isLandscape) {
        if (isLandscape) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    baseColor.copy(alpha = 0.5f),
                    baseColor
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    baseColor.copy(alpha = 0.5f),
                    baseColor
                )
            )
        }
    }

    Box(
        modifier = if (isLandscape) {
            modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .background(brush = gradientBackground)
        } else {
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(brush = gradientBackground)
        },
        contentAlignment = if (isLandscape) Alignment.BottomEnd else Alignment.Center
    ) {
        Column(
            modifier = if (isLandscape) {
                Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(WindowInsetsSides.End)
                    )
                    .padding(start = 64.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, top = 52.dp, end = 20.dp, bottom = 44.dp)
            },
            horizontalAlignment = if (isLandscape) Alignment.End else Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.spacedBy(16.dp, Alignment.Bottom) else Arrangement.spacedBy(16.dp)
        ) {
            // Button 1: Shuffle Toggle (icon + text, content width)
            FilledTonalButton(
                onClick = onShuffleToggle,
                enabled = isShuffleEnabled,
                modifier = Modifier
                    .wrapContentWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isShuffleOn) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (isShuffleOn) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                        alpha = 0.5f
                    ),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isShuffleOn) Icons.Rounded.ShuffleOn else Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isShuffleOn) "Shuffle On" else "Shuffle Off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Button 2: Clear All (icon + text, content width, destructive styling)
            FilledTonalButton(
                onClick = onClearQueue,
                enabled = isClearEnabled,
                modifier = Modifier
                    .wrapContentWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.5f
                    ),
                    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(
                        alpha = 0.38f
                    )
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ClearAll,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetMaxWidth = Dp.Unspecified,
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        QueueMultiSelectionBottomSheetContent(
            selectedItems = selectedItems,
            onDismiss = onDismiss,
            onRemoveFromQueue = onRemoveFromQueue,
            onRemoveOthersFromQueue = onRemoveOthersFromQueue,
            onPlayNext = onPlayNext
        )
    }
}

@Composable
fun QueueMultiSelectionBottomSheetContent(
    selectedItems: List<MediaItem>,
    onDismiss: () -> Unit = {},
    onRemoveFromQueue: () -> Unit = {},
    onRemoveOthersFromQueue: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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

            // Row 2: Play next
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

        SimpleScrollBar(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd),
            paddingEnd = 8.dp
        )
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

private val samplePreviewQueueItems = persistentListOf(
    MediaItem.Builder()
        .setMediaId("1")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Midnight City")
                .setArtist("M83")
                .build()
        )
        .build(),
    MediaItem.Builder()
        .setMediaId("2")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Get Lucky")
                .setArtist("Daft Punk ft. Pharrell Williams")
                .build()
        )
        .build(),
    MediaItem.Builder()
        .setMediaId("3")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Starboy")
                .setArtist("The Weeknd")
                .build()
        )
        .build()
)

private val samplePreviewQueueState = QueueUiState(
    items = samplePreviewQueueItems,
    currentIndex = 0
)

private val samplePreviewQueueActions = object : QueueControls {
    override fun addToQueue(mediaItem: MediaItem) {}
    override fun onSkipToQueueItem(index: Int) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {}
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 1: Standard Playing Queue Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "1. Queue Screen - Standard",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun QueueScreenStandardPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            QueueBottomSheetContent(
                queueState = samplePreviewQueueState,
                actions = samplePreviewQueueActions,
                isShuffleOn = false
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 2: Multi-Select Mode Queue Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "2. Queue Screen - Multi Select Mode",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun QueueScreenMultiSelectPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            QueueBottomSheetContent(
                queueState = samplePreviewQueueState,
                actions = samplePreviewQueueActions,
                isShuffleOn = false,
                initialSelectedIndices = setOf(0, 1)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 3: Multi-Select Bottom Sheet
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "3. Multi Select Bottom Sheet",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun QueueMultiSelectBottomSheetPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                QueueMultiSelectionBottomSheetContent(
                    selectedItems = samplePreviewQueueItems.take(2)
                )
            }
        }
    }
}
