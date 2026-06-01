package com.flash.draft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.flash.draft.ui.home.DashboardScreen // We are about to build this!

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the Multi-Project Repository
        val repo = NoteRepository(this)

        setContent {
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
                            onProjectClick = { projectId ->
                                navController.navigate("canvas/$projectId")
                            },
                            onCreateNewProject = {
                                val newProjectId = repo.createNewProject()
                                navController.navigate("canvas/$newProjectId")
                            }
                        )
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
                                    return CanvasViewModel(repo, projectId) as T
                                }
                            }
                        )

                        DraftCanvas(viewModel = canvasViewModel)
                    }
                }
            }
        }
    }
}