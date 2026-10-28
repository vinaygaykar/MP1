package io.github.vinaygaykar.mp1.ui.library.v2

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinaygaykar.mp1.domain.models.Playlist
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

/**
 * Reusable category item component for Level 1 Category Menu.
 */
@Composable
internal fun CategoryCardItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") subtitle: String? = null,
    @Suppress("UNUSED_PARAMETER") icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = titleModifier
        )
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
    val headerHeightDp = screenHeightDp * 0.28f

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

            // Hero Header Space (28% screen height) containing MP.1 Title + Search & Settings Buttons
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeightDp)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                val maxW = this.maxWidth
                val maxH = this.maxHeight
                val fontSize = minOf(maxW.value * 0.22f, maxH.value * 0.45f, 76f).sp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "MP.1",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
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
            }

            // Category List Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                CategoryCardItem(
                    title = LibraryCategory.PLAYLISTS.title,
                    onClick = { onOpenCategory(LibraryCategory.PLAYLISTS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.PLAYLISTS] ?: Modifier
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                CategoryCardItem(
                    title = LibraryCategory.SONGS.title,
                    onClick = { onOpenCategory(LibraryCategory.SONGS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.SONGS] ?: Modifier
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                CategoryCardItem(
                    title = LibraryCategory.ALBUMS.title,
                    onClick = { onOpenCategory(LibraryCategory.ALBUMS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.ALBUMS] ?: Modifier
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                CategoryCardItem(
                    title = LibraryCategory.ARTISTS.title,
                    onClick = { onOpenCategory(LibraryCategory.ARTISTS) },
                    titleModifier = categoryTitleModifiers[LibraryCategory.ARTISTS] ?: Modifier
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
