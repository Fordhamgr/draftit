package com.flash.draft

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flash.draft.data.NoteRepository
import com.flash.draft.ui.canvas.CanvasViewModel
import com.flash.draft.ui.canvas.DraftCanvas
import com.flash.draft.ui.home. DashboardScreen // We are about to build this!
import com.flash.draft.ui.home.AboutScreen

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun AdaptiveLayout(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val currentDensity = LocalDensity.current
    
    val customDensity = if (configuration.screenWidthDp >= 600) {
        // Use native density on tablet to prevent UI from scaling too large
        currentDensity
    } else {
        // 411f is the baseline width (e.g., Pixel phone).
        val scaleFactor = configuration.screenWidthDp / 411f 
        val clampedScale = scaleFactor.coerceIn(0.85f, 1.2f) 
        Density(
            density = currentDensity.density * clampedScale,
            fontScale = currentDensity.fontScale * clampedScale
        )
    }
    
    CompositionLocalProvider(LocalDensity provides customDensity) {
        content()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide system bars for true immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 1. Initialize the Repositories
        val repo = NoteRepository(this)
        val settingsRepo = com.flash.draft.data.SettingsRepository(this)

        setContent {
            val isDarkModeFlow = androidx.compose.runtime.remember { settingsRepo.getDarkModeFlow() }
            val isDarkMode by isDarkModeFlow.collectAsStateWithLifecycle(initialValue = false)
            
            val isPerformanceMode by settingsRepo.getPerformanceModeFlow().collectAsStateWithLifecycle(initialValue = false)
            val framerate by settingsRepo.getFramerateFlow().collectAsStateWithLifecycle(initialValue = 60)
            val isHapticFeedbackEnabled by settingsRepo.getHapticFeedbackFlow().collectAsStateWithLifecycle(initialValue = true)
            


            // Override the MaterialTheme color scheme based on user setting
            val colorScheme = if (isDarkMode) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()

            val nativeHaptic = LocalHapticFeedback.current
            val customHaptic = remember(isHapticFeedbackEnabled, nativeHaptic) {
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        if (isHapticFeedbackEnabled) {
                            nativeHaptic.performHapticFeedback(hapticFeedbackType)
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalHapticFeedback provides customHaptic) {
                MaterialTheme(colorScheme = colorScheme) {
                AdaptiveLayout {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // 2. Setup Jetpack Navigation
                        val navController = rememberNavController()

                        androidx.compose.runtime.DisposableEffect(navController) {
                            val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                                com.flash.draft.utils.DraftLogger.logNav(destination.route)
                            }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose {
                                navController.removeOnDestinationChangedListener(listener)
                            }
                        }

                        NavHost(navController = navController, startDestination = "dashboard") {

                            // ROUTE 1: The Glassmorphism Homescreen
                            composable("dashboard") {
                                DashboardScreen(
                                    repository = repo,
                                    isDarkTheme = isDarkMode,
                                    onProjectClick = { projectId ->
                                        navController.navigate("canvas/$projectId") { launchSingleTop = true }
                                    },
                                    onCreateNewProject = { title ->
                                        val newProjectId = repo.createNewProject(title)
                                        navController.navigate("canvas/$newProjectId") { launchSingleTop = true }
                                    },
                                    onNavigateToAbout = {
                                        navController.navigate("about") { launchSingleTop = true }
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings") { launchSingleTop = true }
                                    }
                                )
                            }

                            // ROUTE: Settings Screen
                            composable("settings") {
                                val settingsViewModel: com.flash.draft.ui.settings.SettingsViewModel = viewModel(
                                    factory = com.flash.draft.ui.settings.SettingsViewModelFactory(settingsRepo)
                                )
                                com.flash.draft.ui.settings.SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { navController.navigateUp() }
                                )
                            }

                            // ROUTE: About Screen
                            composable("about") {
                                AboutScreen(isDarkTheme = isDarkMode, onBack = { navController.navigateUp() })
                            }

                            // ROUTE 2: The Infinite Canvas
                            composable(
                                route = "canvas/{projectId}",
                                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable

                                // Build a dedicated Brain for THIS specific project
                                val canvasViewModel: CanvasViewModel = viewModel(
                                    key = projectId, // Ensures a fresh ViewModel if you switch projects
                                    factory = object : ViewModelProvider.Factory {
                                        @Suppress("UNCHECKED_CAST")
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            return CanvasViewModel(repo, settingsRepo, projectId) as T
                                        }
                                    }
                                )

                                DraftCanvas(
                                    viewModel = canvasViewModel,
                                    onBack = { navController.navigateUp() },
                                    onNavigateToBoard = { boardId -> navController.navigate("canvas/$boardId") }
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