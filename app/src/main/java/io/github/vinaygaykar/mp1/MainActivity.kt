package io.github.vinaygaykar.mp1

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.vinaygaykar.mp1.playback.PlayerViewModel
import io.github.vinaygaykar.mp1.ui.nowplaying.NowPlayingScreen
import io.github.vinaygaykar.mp1.ui.library.v2.LibraryScreenV2
import io.github.vinaygaykar.mp1.ui.search.SearchScreen
import io.github.vinaygaykar.mp1.ui.settings.ScanLocationScreen
import io.github.vinaygaykar.mp1.ui.settings.SettingsScreen
import io.github.vinaygaykar.mp1.ui.settings.SettingsViewModel
import io.github.vinaygaykar.mp1.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Library : Screen("library", "Library", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val progressState by playerViewModel.progressState.collectAsState()
    val queueState by playerViewModel.queueState.collectAsState()
    val lyricsState by playerViewModel.lyricsState.collectAsState()
    val visualizerState by playerViewModel.visualizerState.collectAsState()
    val hasScanFoldersState by settingsViewModel.hasScanFolders.collectAsState()

    if (hasScanFoldersState == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val context = LocalContext.current
    val initialHasFolders = remember { hasScanFoldersState == true }
    val startDestination = if (initialHasFolders) Screen.Library.route else "onboarding_scan_locations"

    val isPlayerActive by remember(playerViewModel) {
        playerViewModel.playbackState.map { it.trackTitle.isNotEmpty() }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val expansionProgress = remember { mutableFloatStateOf(0f) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = (if (isPlayerActive) 72.dp + 16.dp + navBarBottom else 0.dp) + 16.dp

    var miniPlayerBulgeTrigger by remember { mutableLongStateOf(0L) }
    var miniPlayerTargetOffset by remember { mutableStateOf<Offset?>(null) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(hasScanFoldersState, currentRoute) {
        if (hasScanFoldersState == true && currentRoute == "onboarding_scan_locations") {
            navController.navigate(Screen.Library.route) {
                popUpTo("onboarding_scan_locations") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(
                            route = Screen.Library.route,
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeOut(tween(350))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeIn(tween(350))
                            }
                        ) { 
                            LibraryScreenV2(
                                bottomPadding = bottomPadding,
                                isPlayerActive = isPlayerActive,
                                miniPlayerTargetOffset = miniPlayerTargetOffset,
                                onBulgeMiniPlayer = { miniPlayerBulgeTrigger = System.currentTimeMillis() },
                                onSearchClick = { navController.navigate(Screen.Search.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onOpenScanLocations = { navController.navigate("scan_locations") }
                            ) 
                        }
                        composable(
                            route = Screen.Search.route,
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn(tween(350))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeOut(tween(350))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeIn(tween(350))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut(tween(350))
                            }
                        ) { 
                            SearchScreen(
                                bottomPadding = bottomPadding,
                                onBack = { navController.popBackStack() }
                            ) 
                        }
                        composable(
                            route = Screen.Settings.route,
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn(tween(350))
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeOut(tween(350))
                            },
                            popEnterTransition = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(350)) + fadeIn(tween(350))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut(tween(350))
                            }
                        ) {
                            SettingsScreen(
                                bottomPadding = bottomPadding,
                                onBack = { navController.popBackStack() },
                                onNavigateToScanLocations = { navController.navigate("scan_locations") }
                            )
                        }
                        composable("scan_locations") {
                            ScanLocationScreen(
                                onBack = { navController.popBackStack() },
                                bottomPadding = bottomPadding,
                                isOnboarding = false
                            )
                        }
                        composable("onboarding_scan_locations") {
                            ScanLocationScreen(
                                onBack = { (context as? Activity)?.finish() },
                                onContinueInBackground = {
                                    navController.navigate(Screen.Library.route) {
                                        popUpTo("onboarding_scan_locations") { inclusive = true }
                                    }
                                },
                                bottomPadding = bottomPadding,
                                isOnboarding = true
                            )
                        }
                    }
                }
            }
        }

        // Now Playing Screen - Sits on top
        NowPlayingScreen(
            modifier = Modifier.fillMaxSize(),
            playbackState = playbackState,
            progressState = progressState,
            queueState = queueState,
            lyricsState = lyricsState,
            playbackControls = playerViewModel,
            queueControls = playerViewModel,
            lyricsControls = playerViewModel,
            onProgressUpdate = { expansionProgress.floatValue = it },
            visualizerState = visualizerState,
            bulgeTrigger = miniPlayerBulgeTrigger
        ) { offset -> miniPlayerTargetOffset = offset }
    }
}
