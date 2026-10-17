package io.github.vinaygaykar.mp1.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.vinaygaykar.mp1.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomPadding: Dp = 0.dp,
    onBack: () -> Unit = {},
    onNavigateToScanLocations: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val useCleanAlbumGrid by viewModel.useCleanAlbumGrid.collectAsState()
    val playbackWave by viewModel.playbackWave.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val titleFontSize = lerp(28.sp, 20.sp, collapsedFraction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(bottom = bottomPadding),
    ) {
        LargeTopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            scrollBehavior = scrollBehavior,
        )
        ListItem(
            headlineContent = { Text("Scan Locations") },
            supportingContent = { Text("Manage folders to scan for music") },
            modifier = Modifier.clickable { onNavigateToScanLocations() },
        )
        Text(
            text = "Experimental",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text("Clean Album Grid") },
            supportingContent = { Text("Toggle between clean square grid and classic layout") },
            trailingContent = {
                Switch(
                    checked = useCleanAlbumGrid,
                    onCheckedChange = { viewModel.setUseCleanAlbumGrid(it) },
                )
            },
            modifier = Modifier.clickable { viewModel.setUseCleanAlbumGrid(!useCleanAlbumGrid) },
        )
        var playbackWaveExpanded by remember { mutableStateOf(false) }
        val playbackWaveOptions = listOf("Frequency", "Energy")

        Box {
            ListItem(
                headlineContent = { Text("Playback wave") },
                supportingContent = { Text(playbackWave) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select playback wave option",
                    )
                },
                modifier = Modifier.clickable { playbackWaveExpanded = true },
            )
            DropdownMenu(
                expanded = playbackWaveExpanded,
                onDismissRequest = { playbackWaveExpanded = false },
            ) {
                playbackWaveOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (option == playbackWave) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == playbackWave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            viewModel.setPlaybackWave(option)
                            playbackWaveExpanded = false
                        },
                    )
                }
            }
        }
        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("${stringResource(R.string.app_name)} v1.0") },
        )
    }
}
