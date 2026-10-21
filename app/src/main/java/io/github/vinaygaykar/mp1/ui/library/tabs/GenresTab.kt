package io.github.vinaygaykar.mp1.ui.library.tabs

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaygaykar.mp1.domain.models.Genre
import io.github.vinaygaykar.mp1.ui.components.SimpleScrollBar

@Composable
fun GenreList(
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (genres.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No genres found.")
        }
    } else {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(genres, key = { it.id }) { genre ->
                    GenreListItem(
                        genre = genre,
                        onClick = { onGenreClick(genre) }
                    )
                }
            }
            SimpleScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(100f)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                listState = listState
            )
        }
    }
}

@Composable
fun GenreListItem(
    genre: Genre,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = genre.name.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
    }
}
