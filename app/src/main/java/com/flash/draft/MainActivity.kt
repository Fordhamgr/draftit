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
            val isMotionBlurEnabled by settingsRepo.getMotionBlurFlow().collectAsStateWithLifecycle(initialValue = true)
            val motionBlurAmount by settingsRepo.getMotionBlurAmountFlow().collectAsStateWithLifecycle(initialValue = 0.5f)
            val isHapticFeedbackEnabled by settingsRepo.getHapticFeedbackFlow().collectAsStateWithLifecycle(initialValue = true)
            
            androidx.compose.runtime.LaunchedEffect(framerate) {
                val params = window.attributes
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val currentDisplay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        display
                    } else {
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay
                    }
                    if (currentDisplay != null) {
                        if (framerate > 0) {
                            val modes = currentDisplay.supportedModes
                            val mode = modes.minByOrNull { kotlin.math.abs(it.refreshRate - framerate.toFloat()) }
                            params.preferredDisplayModeId = mode?.modeId ?: 0
                        } else {
                            params.preferredDisplayModeId = 0
                        }
                        window.attributes = params
                    }
                }
            }

            // Override the MaterialTheme color scheme based on user setting
            val colorScheme = if (isDarkMode) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                AdaptiveLayout {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // 2. Setup Jetpack Navigation
                        val navController = rememberNavController()

                        NavHost(navController = navController, startDestination = "dashboard") {

                            // ROUTE 1: The Glassmorphism Homescreen
                            composable("dashboard") {
                                DashboardScreen(
                                    repository = repo,
                                    isDarkTheme = isDarkMode,
                                    onProjectClick = { projectId ->
                                        navController.navigate("canvas/$projectId")
                                    },
                                    onCreateNewProject = { title ->
                                        val newProjectId = repo.createNewProject(title)
                                        navController.navigate("canvas/$newProjectId")
                                    },
                                    onNavigateToAbout = {
                                        navController.navigate("about")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
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
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // ROUTE: About Screen
                            composable("about") {
                                AboutScreen(onBack = { navController.popBackStack() })
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
                                    isDarkTheme = isDarkMode,
                                    isPerformanceMode = isPerformanceMode,
                                    isMotionBlurEnabled = isMotionBlurEnabled,
                                    motionBlurAmount = motionBlurAmount,
                                    isHapticFeedbackEnabled = isHapticFeedbackEnabled,
                                    onBack = { navController.popBackStack() },
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