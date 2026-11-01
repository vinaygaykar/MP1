package io.github.vinaygaykar.mp1.ui.nowplaying

import android.content.res.Configuration
import io.github.vinaygaykar.mp1.data.repo.LyricsParser
import io.github.vinaygaykar.mp1.data.repo.isInstrumental
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.vinaygaykar.mp1.domain.models.Lyrics
import io.github.vinaygaykar.mp1.playback.LyricsControls
import io.github.vinaygaykar.mp1.playback.LyricsUiState
import io.github.vinaygaykar.mp1.playback.PlaybackProgressState
import io.github.vinaygaykar.mp1.playback.PlaybackUiState
import io.github.vinaygaykar.mp1.utils.collectArtistNames
import io.github.vinaygaykar.mp1.utils.joinArtistsForDisplay
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(
    lyricsState: LyricsUiState,
    playbackState: PlaybackUiState,
    progressState: PlaybackProgressState,
    actions: LyricsControls,
    onSeek: (Long) -> Unit,
    initialShowSearchDialog: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val lyrics = remember(lyricsState) {
        if (lyricsState is LyricsUiState.Success) {
            LyricsParser.parse(lyricsState.lyrics, lyricsState.isSynced)
        } else null
    }

    var showSearchDialog by remember { mutableStateOf(initialShowSearchDialog) }

    if (showSearchDialog) {
        LyricsSearchDialog(
            initialQuery = "${playbackState.trackTitle} ${playbackState.artist}",
            onSearch = { query ->
                actions.onLyricsSearch(query)
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
                lyricsState is LyricsUiState.Loading -> {
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
                        currentPositionMs = progressState.positionMs,
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
                        model = playbackState.artworkUri,
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
                        val displayArtist = remember(playbackState.artist, playbackState.trackTitle) {
                            collectArtistNames(playbackState.artist, playbackState.trackTitle).joinArtistsForDisplay()
                        }

                        Text(
                            text = playbackState.trackTitle,
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

                    Spacer(modifier = Modifier.width(8.dp))

                    DismissIconButton(onDismiss = onDismiss)
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
                    FilledTonalButton(
                        onClick = { showSearchDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right: Offline Badge or Save Button
                    Box {
                        if (lyricsState is LyricsUiState.Success && lyricsState.lyrics.isNotBlank()) {
                            if (lyricsState.isLocal) {
                                FilledTonalButton(
                                    onClick = {},
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Offline",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { actions.onSaveLyrics() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Save",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        contentPadding = PaddingValues(top = 180.dp, bottom = 100.dp, start = 36.dp, end = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeIndex

            if (line.isInstrumental()) {
                BouncingMusicalNotes(
                    isActive = isActive,
                    onClick = { onSeek(line.timestampMs) }
                )
            } else {
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
}

@Composable
fun BouncingMusicalNotes(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_notes_transition")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val noteCount = 3
        for (i in 0 until noteCount) {
            val offsetY by if (isActive) {
                infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 500,
                            easing = FastOutSlowInEasing,
                            delayMillis = i * 150
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "note_bounce_$i"
                )
            } else {
                remember { mutableFloatStateOf(0f) }
            }

            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.4f,
                label = "note_alpha_$i"
            )

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1f,
                label = "note_scale_$i"
            )

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Instrumental break",
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (isActive) 28.dp else 22.dp)
                    .graphicsLayer {
                        translationY = offsetY
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
            .padding(horizontal = 36.dp)
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

private val samplePreviewPlaybackState = PlaybackUiState(
    trackTitle = "Midnight City",
    artist = "M83",
    album = "Hurry Up, We're Dreaming"
)

private val samplePreviewProgressState = PlaybackProgressState(
    positionMs = 30000L,
    durationMs = 240000L
)

private val samplePreviewLyricsControls = object : LyricsControls {
    override fun onLyricsSearch(query: String) {}
    override fun onSaveLyrics() {}
}

private val sampleSyncedLyrics = """
    [00:10.00] You saw me standing alone
    [00:20.00] Without a dream in my heart
    [00:30.00] Without a love of my own
    [00:40.00] ♪
    [00:50.00] And then there was you
""".trimIndent()

private val sampleStaticLyrics = """
    You saw me standing alone
    Without a dream in my heart
    Without a love of my own
    
    And then there was you
    The moon turned to gold
    Here in my arms
""".trimIndent()

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 1: Synced Lyrics Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "1. Lyrics Screen - Synced",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "1. Lyrics Screen - Synced",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "1. Lyrics Screen - Synced",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "1. Lyrics Screen - Synced",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LyricsScreenSyncedPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LyricsScreen(
                lyricsState = LyricsUiState.Success(
                    lyrics = sampleSyncedLyrics,
                    isSynced = true,
                    isLocal = true
                ),
                playbackState = samplePreviewPlaybackState,
                progressState = samplePreviewProgressState,
                actions = samplePreviewLyricsControls,
                onSeek = {}
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 2: Static Lyrics Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "2. Lyrics Screen - Static",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "2. Lyrics Screen - Static",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "2. Lyrics Screen - Static",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "2. Lyrics Screen - Static",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LyricsScreenStaticPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LyricsScreen(
                lyricsState = LyricsUiState.Success(
                    lyrics = sampleStaticLyrics,
                    isSynced = false,
                    isLocal = false
                ),
                playbackState = samplePreviewPlaybackState,
                progressState = samplePreviewProgressState,
                actions = samplePreviewLyricsControls,
                onSeek = {}
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 3: Empty Lyrics Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "3. Lyrics Screen - Empty",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "3. Lyrics Screen - Empty",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "3. Lyrics Screen - Empty",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "3. Lyrics Screen - Empty",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LyricsScreenEmptyPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LyricsScreen(
                lyricsState = LyricsUiState.None,
                playbackState = samplePreviewPlaybackState,
                progressState = samplePreviewProgressState,
                actions = samplePreviewLyricsControls,
                onSeek = {}
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 4: Search Lyrics Dialog Screen
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "4. Lyrics Screen - Search Dialog",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "4. Lyrics Screen - Search Dialog",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "4. Lyrics Screen - Search Dialog",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "4. Lyrics Screen - Search Dialog",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LyricsScreenSearchDialogPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LyricsScreen(
                lyricsState = LyricsUiState.Success(
                    lyrics = sampleSyncedLyrics,
                    isSynced = true,
                    isLocal = true
                ),
                playbackState = samplePreviewPlaybackState,
                progressState = samplePreviewProgressState,
                actions = samplePreviewLyricsControls,
                onSeek = {},
                initialShowSearchDialog = true
            )
        }
    }
}
