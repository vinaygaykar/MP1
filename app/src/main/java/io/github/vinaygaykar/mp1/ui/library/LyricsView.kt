package io.github.vinaygaykar.mp1.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import io.github.vinaygaykar.mp1.playback.PlayerUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    state: PlayerUiState,
    onSeek: (Long) -> Unit,
    onSearch: (String) -> Unit
) {
    val lyrics = remember(state.lyrics, state.isLyricsSynced) {
        state.lyrics?.let { LyricsParser.parse(it, state.isLyricsSynced) }
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
            color = MaterialTheme.colorScheme.surface
        ) {
            when {
                state.lyricsLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                lyrics == null || (lyrics.content.isEmpty() && lyrics.lines.isEmpty()) -> {
                    LyricsEmptyState(
                        title = state.trackTitle,
                        artist = state.artist,
                        onSearch = onSearch,
                        error = state.lyricsError,
                        modifier = Modifier.statusBarsPadding()
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

        // Overlay Header Layer (Dynamic Container)
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 24.dp, start = 32.dp, end = 32.dp)
                .align(Alignment.TopCenter)
                .height(64.dp)
                .widthIn(max = 400.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 12.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.trackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val rotation = (state.positionMs * 0.12f) % 360f
                AsyncImage(
                    model = state.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
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
        contentPadding = PaddingValues(top = 120.dp, bottom = 100.dp, start = 24.dp, end = 24.dp),
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
        Spacer(modifier = Modifier.height(120.dp))
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
    title: String,
    artist: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var searchQuery by remember { mutableStateOf("$title $artist") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (error != null) "Error loading lyrics" else "Lyrics not found",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (error != null) error else "We couldn't find lyrics for \"$title\" by $artist.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search lyrics") },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { if (searchQuery.isNotBlank()) onSearch(searchQuery) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { if (searchQuery.isNotBlank()) onSearch(searchQuery) },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        ) {
            Text("Search on LRCLIB")
        }
    }
}
