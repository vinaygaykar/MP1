package io.github.vinaygaykar.mp1.ui.library.tabs

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vinaygaykar.mp1.domain.models.Album
import io.github.vinaygaykar.mp1.ui.album.AlbumDetailActivity
import io.github.vinaygaykar.mp1.ui.components.EmptyState
import io.github.vinaygaykar.mp1.ui.library.launchActivityWithScaleUp
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

private const val EMPTY_STATE_MESSAGE = "No Albums found.\nScan a folder from Settings."

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AlbumsTab(
    albums: List<Album>,
    contentPadding: PaddingValues = PaddingValues(),
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    if (albums.isEmpty()) {
        EmptyState(
            message = EMPTY_STATE_MESSAGE,
            modifier = Modifier.padding(contentPadding)
        )
        return
    }

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

    val onAlbumClick = rememberOnAlbumClick()

    when {
        isWideWindow -> {
            AlbumsTabLandscape(
                albums = albums,
                onAlbumClick = onAlbumClick,
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                contentPadding = contentPadding
            )
        }
        isCompactHeight -> {
            AlbumsTabCompact(
                albums = albums,
                onAlbumClick = onAlbumClick,
                contentPadding = contentPadding
            )
        }
        else -> {
            AlbumsTabPortrait(
                albums = albums,
                onAlbumClick = onAlbumClick,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun rememberOnAlbumClick(): (Album) -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        { album ->
            val intent = Intent(context, AlbumDetailActivity::class.java).apply {
                putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id)
            }
            launchActivityWithScaleUp(context = context, intent = intent, view = view)
        }
    }
}

@Composable
private fun AlbumsTabPortrait(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    AlbumGridPunk(
        albums = albums,
        onAlbumClick = onAlbumClick,
        contentPadding = contentPadding
    )
}

@Composable
private fun AlbumsTabCompact(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    AlbumGridClean(
        albums = albums,
        onAlbumClick = { album, _, _ -> onAlbumClick(album) },
        contentPadding = contentPadding
    )
}

@Composable
private fun AlbumsTabLandscape(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Left Pane (occupies available width)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
        ) {
            AlbumGridCompact(
                albums = albums,
                onAlbumClick = onAlbumClick,
                contentPadding = contentPadding
            )
        }

        // Right Pane (occupies required width of the elements)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchIconButton(onClick = onSearchClick)
                SettingsIconButton(onClick = onSettingsClick)
            }
        }
    }
}

@Composable
private fun SearchIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val sampleAlbums = listOf(
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

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 1: Albums Tab - Content
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "1. Albums Tab - Content",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun AlbumsTabContentPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumsTab(
                albums = sampleAlbums
            )
        }
    }
}

// -----------------------------------------------------------------------------
// PREVIEW CATEGORY 2: Albums Tab - Empty State
// -----------------------------------------------------------------------------

@Preview(
    name = "Portrait Light",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Portrait Dark",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Landscape Light",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Dark",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Portrait Split",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=411dp,height=445dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Landscape Split",
    group = "2. Albums Tab - Empty State",
    showBackground = true,
    device = "spec:width=445dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun AlbumsTabEmptyStatePreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumsTab(
                albums = emptyList()
            )
        }
    }
}

