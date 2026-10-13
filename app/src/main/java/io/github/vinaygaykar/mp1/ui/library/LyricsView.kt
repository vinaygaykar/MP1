package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.playback.PlayerActions
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    state: PlayerUiState,
    actions: PlayerActions,
    onSeek: (Long) -> Unit,
    onSearch: (String) -> Unit
) {
    val lyrics = remember(state.lyrics, state.isLyricsSynced) {
        state.lyrics?.let { LyricsParser.parse(it, state.isLyricsSynced) }
    }

    var showSearchDialog by remember { mutableStateOf(false) }

    if (showSearchDialog) {
        LyricsSearchDialog(
            initialQuery = "${state.trackTitle} ${state.artist}",
            onSearch = { query ->
                onSearch(query)
                showSearchDialog = false
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
    ) {
        // Content Layer
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            when {
                state.lyricsLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                lyrics == null || (lyrics.content.isEmpty() && lyrics.lines.isEmpty()) -> {
                    LyricsEmptyState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                lyrics.isSynced -> {
                    SyncedLyrics(
                        lyrics = lyrics,
                        currentPositionMs = state.positionMs,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    StaticLyrics(
                        content = lyrics.content,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Sticky Top Header (Dynamic surface color from seed palette, no alpha)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Top Track Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val displayArtist = remember(state.artist, state.trackTitle) {
                            collectArtistNames(state.artist, state.trackTitle).joinArtistsForDisplay()
                        }

                        Text(
                            text = state.trackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Row (Left: Search Button, Right: Offline Badge or Save Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Search Button
                    AssistChip(
                        onClick = { showSearchDialog = true },
                        label = { Text("Search") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search lyrics",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = CircleShape
                    )

                    // Right: Offline Badge or Save Button
                    Box {
                        if (!state.lyrics.isNullOrBlank()) {
                            if (state.isLyricsLocal) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Offline") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = "Offline lyrics",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = null,
                                    shape = CircleShape
                                )
                            } else {
                                AssistChip(
                                    onClick = { actions.onSaveLyrics() },
                                    label = { Text("Save") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Save lyrics",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = null,
                                    shape = CircleShape
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsSearchDialog(
    initialQuery: String,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Lyrics") },
        text = {
            Column {
                Text(
                    text = "Search LRCLIB for lyrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search query") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                onSearch(searchQuery)
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        onSearch(searchQuery)
                    }
                }
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SyncedLyrics(
    lyrics: Lyrics,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val activeIndex = remember(currentPositionMs, lyrics.lines) {
        val index = lyrics.lines.indexOfLast { it.timestampMs <= currentPositionMs }
        if (index == -1) 0 else index
    }

    LaunchedEffect(activeIndex) {
        coroutineScope.launch {
            if (activeIndex >= 0) {
                listState.animateScrollToItem(
                    index = (activeIndex - 2).coerceAtLeast(0),
                    scrollOffset = 0
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 180.dp, bottom = 100.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                label = "lyric_alpha"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 1f,
                label = "lyric_scale"
            )

            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    fontSize = if (isActive) 28.sp else 20.sp,
                    lineHeight = if (isActive) 36.sp else 28.sp
                ),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timestampMs) }
                    .padding(vertical = 12.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

@Composable
fun StaticLyrics(content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(180.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 36.sp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun LyricsEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lyrics not found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
