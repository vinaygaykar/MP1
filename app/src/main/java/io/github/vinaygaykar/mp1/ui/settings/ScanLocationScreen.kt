package io.github.vinaygaykar.mp1.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.vinaygaykar.mp1.data.local.entities.ScanFolderEntity
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.components.NoticeBanner
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme

@Composable
fun ScanLocationScreen(
    onBack: () -> Unit,
    onContinueInBackground: () -> Unit = {},
    bottomPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    isOnboarding: Boolean = false,
) {
    val context = LocalContext.current
    val folders by viewModel.scanFolders.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val currentFolder by viewModel.currentScanningFolder.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val queueState by playerViewModel.queueState.collectAsState()

    val showNotice = playbackState.isPlaying || queueState.items.isNotEmpty()

    BackHandler {
        if (isOnboarding) {
            (context as? Activity)?.finish()
        } else {
            onBack()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.addFolder(it.toString(), it.lastPathSegment ?: "Music Folder")
        }
    }

    ScanLocationContent(
        folders = folders,
        isScanning = isScanning,
        currentFolder = currentFolder,
        showNotice = showNotice,
        onBack = {
            if (isOnboarding) {
                (context as? Activity)?.finish()
            } else {
                onBack()
            }
        },
        onStartScan = { viewModel.startScan() },
        onAddFolderClick = { launcher.launch(null) },
        onRemoveFolder = { viewModel.removeFolder(it) },
        onContinueInBackground = onContinueInBackground,
        bottomPadding = bottomPadding,
        isOnboarding = isOnboarding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanLocationContent(
    folders: List<ScanFolderEntity>,
    isScanning: Boolean,
    currentFolder: String?,
    showNotice: Boolean,
    onBack: () -> Unit,
    onStartScan: () -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onRemoveFolder: (String) -> Unit = {},
    onContinueInBackground: () -> Unit = {},
    bottomPadding: Dp = 0.dp,
    isOnboarding: Boolean = false,
    initialShowScanDialog: Boolean = true,
) {
    var showScanDialog by remember { mutableStateOf(initialShowScanDialog) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val titleFontSize = lerp(28.sp, 20.sp, collapsedFraction)

    val displayFolder = remember(currentFolder) {
        currentFolder
            ?.removePrefix("primary:")
            ?.removePrefix("primary")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.replace("/", "/\u200B")
            ?.replace(":", ":\u200B")
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(bottom = bottomPadding),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = if (isOnboarding) "Select Music Folder" else "Scan Directories",
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
                actions = {
                    FilledIconButton(
                        onClick = {
                            if (!isScanning && folders.isNotEmpty()) {
                                showScanDialog = true
                                onStartScan()
                            }
                        },
                        enabled = !isScanning && folders.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Now",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isScanning) {
                        showScanDialog = true
                        onAddFolderClick()
                    }
                },
                containerColor = if (isScanning) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (isScanning) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Folder",
                )
            }
        },
    ) { padding ->
        if (isScanning && folders.isNotEmpty() && showScanDialog) {
            AlertDialog(
                onDismissRequest = {
                    showScanDialog = false
                    onContinueInBackground()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showScanDialog = false
                            onContinueInBackground()
                        }
                    ) {
                        Text("Continue in background")
                    }
                },
                title = { Text("Scanning Library") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 8.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = displayFolder ?: "Please wait while we index your music...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showNotice) {
                NoticeBanner(
                    title = "Any changes on this screen will stop current song and clear your current playing queue.",
                    showBottomDivider = isScanning,
                )
            }

            if (isScanning) {
                NoticeBanner(
                    title = "Library scan in progress...",
                    subtitle = if (!displayFolder.isNullOrBlank()) "Scanning $displayFolder" else null,
                    trailingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                )
            }

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isOnboarding) "Welcome! Choose a folder to begin." else "No scan directories added.",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button below to add a folder containing your music.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(folders) { folder ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = folder.displayName
                                        .removePrefix("primary:")
                                        .removePrefix("primary"),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        if (!isScanning) {
                                            onRemoveFolder(folder.id)
                                        }
                                    },
                                    enabled = !isScanning,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = if (isScanning) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Scan Location - Banners Active", showBackground = true)
@Composable
private fun ScanLocationScreenWithBannersPreview() {
    MusicPlayerTheme {
        ScanLocationContent(
            folders = listOf(
                ScanFolderEntity(
                    id = "1",
                    treeUri = "content://...",
                    displayName = "primary:Music",
                    dateAdded = System.currentTimeMillis(),
                    lastScanned = null,
                    isActive = true,
                ),
            ),
            isScanning = true,
            currentFolder = "/storage/emulated/0/Music",
            showNotice = true,
            initialShowScanDialog = false,
            onBack = {},
        )
    }
}
