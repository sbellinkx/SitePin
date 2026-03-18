package com.sitepinapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sitepinapp.ui.screens.DocumentListScreen
import com.sitepinapp.ui.screens.PlanAnnotationScreen
import com.sitepinapp.ui.screens.ProjectListScreen

sealed class Screen(val route: String) {
    data object ProjectList : Screen("projects")
    data object DocumentList : Screen("projects/{projectId}/documents") {
        fun createRoute(projectId: Long) = "projects/$projectId/documents"
    }
    data object PlanAnnotation : Screen("documents/{documentId}/annotate") {
        fun createRoute(documentId: Long) = "documents/$documentId/annotate"
    }
}

@Composable
fun SitePinNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.ProjectList.route) {
        composable(Screen.ProjectList.route) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Screen.DocumentList.createRoute(projectId))
                }
            )
        }
        composable(
            route = Screen.DocumentList.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            DocumentListScreen(
                projectId = projectId,
                onDocumentClick = { docId ->
                    navController.navigate(Screen.PlanAnnotation.createRoute(docId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PlanAnnotation.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            PlanAnnotationScreen(
                documentId = documentId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
