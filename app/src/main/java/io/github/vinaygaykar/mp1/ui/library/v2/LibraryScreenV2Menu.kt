package io.github.vinaygaykar.mp1.ui.library.v2

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

/**
 * Reusable category card item component for Level 1 Category Menu.
 */
@Composable
internal fun CategoryCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = titleModifier
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Level 1 Screen: Category Selection Menu.
 */
@Composable
internal fun CategoryMenuScreen(
    uiState: LibraryUiState,
    displayFolder: String?,
    bottomPadding: Dp,
    detailProgress: Float,
    screenWidthPx: Float,
    onOpenCategory: (LibraryCategory) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenScanLocations: () -> Unit,
    modifier: Modifier = Modifier,
    categoryTitleModifiers: Map<LibraryCategory, Modifier> = emptyMap()
) {
    val configuration = LocalConfiguration.current

    val screenHeightDp = configuration.screenHeightDp.dp
    val headerHeightDp = screenHeightDp * 0.30f

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = -(detailProgress * screenWidthPx / 3f)
                alpha = 1f - (detailProgress * 0.3f)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = bottomPadding)
        ) {
            // Scan Notice Banner
            AnimatedVisibility(
                visible = uiState.isScanning,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                NoticeBanner(
                    title = "Library scan in progress...",
                    subtitle = if (!displayFolder.isNullOrBlank()) "Scanning $displayFolder" else null,
                    onClick = onOpenScanLocations,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Scan Directories",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            // Level 1 Hero Header (30% screen height, fixed, non-collapsible, scrolls with content)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeightDp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxW = this.maxWidth
                val maxH = this.maxHeight
                val fontSize = minOf(maxW.value * 0.30f, maxH.value * 0.70f).sp

                Text(
                    text = "MP.1",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = 10.dp)
                )
            }

            // Scrollable Content (Search & Settings buttons + Category Cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search and Settings buttons (moved outside the action bar for Level 1 screen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onOpenCategory(LibraryCategory.SEARCH) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            onClick = onSettingsClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                CategoryCardItem(
                    title = LibraryCategory.PLAYLISTS.title,
                    subtitle = "${uiState.playlists.size} playlists",
                    icon = Icons.Default.LibraryMusic,
                    onClick = { onOpenCategory(LibraryCategory.PLAYLISTS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.PLAYLISTS] ?: Modifier
                )

                CategoryCardItem(
                    title = LibraryCategory.SONGS.title,
                    subtitle = "${uiState.songs.size} tracks",
                    icon = Icons.Default.MusicNote,
                    onClick = { onOpenCategory(LibraryCategory.SONGS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.SONGS] ?: Modifier
                )

                CategoryCardItem(
                    title = LibraryCategory.ALBUMS.title,
                    subtitle = "${uiState.albums.size} albums",
                    icon = Icons.Default.Album,
                    onClick = { onOpenCategory(LibraryCategory.ALBUMS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.ALBUMS] ?: Modifier
                )

                CategoryCardItem(
                    title = LibraryCategory.ARTISTS.title,
                    subtitle = "${uiState.artists.size} artists",
                    icon = Icons.Default.Person,
                    onClick = { onOpenCategory(LibraryCategory.ARTISTS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.ARTISTS] ?: Modifier
                )
            }
        }
    }
}

@Preview(
    name = "Category Menu - Portrait Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Category Menu - Portrait Dark",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Category Menu - Landscape Light",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Category Menu - Landscape Dark",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CategoryMenuScreenPreview() {
    MusicPlayerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CategoryMenuScreen(
                uiState = LibraryUiState(
                    playlists = listOf(Playlist(id = 1L, name = "Favorites", songCount = 12)),
                    isScanning = false
                ),
                displayFolder = null,
                bottomPadding = 0.dp,
                detailProgress = 0f,
                screenWidthPx = 1080f,
                onOpenCategory = {},
                onSettingsClick = {},
                onOpenScanLocations = {}
            )
        }
    }
}
