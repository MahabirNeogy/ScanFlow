package com.mahabirneogy.scanflow.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mahabirneogy.scanflow.ui.screens.about.AboutScreen
import com.mahabirneogy.scanflow.ui.screens.camera.CameraScreen
import com.mahabirneogy.scanflow.ui.screens.detail.DocumentDetailScreen
import com.mahabirneogy.scanflow.ui.screens.edit.PageEditScreen
import com.mahabirneogy.scanflow.ui.screens.files.FilesScreen
import com.mahabirneogy.scanflow.ui.screens.preview.PreviewScreen
import com.mahabirneogy.scanflow.ui.screens.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("files") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("files") {
            FilesScreen(
                onScanClick = { navController.navigate("camera") },
                onDocumentClick = { docId -> navController.navigate("detail/$docId") },
                onInfoClick = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = "detail/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            val refreshKey by backStackEntry.savedStateHandle
                .getStateFlow("refreshKey", 0L)
                .collectAsState()
            DocumentDetailScreen(
                documentId = documentId,
                onBackClick = { navController.popBackStack() },
                onPageClick = { pageIndex -> navController.navigate("edit/$documentId/$pageIndex") },
                onAddPageClick = { navController.navigate("addpage/$documentId") },
                refreshKey = refreshKey
            )
        }
        composable(
            route = "edit/{documentId}/{pageIndex}",
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: return@composable
            PageEditScreen(
                documentId = documentId,
                pageIndex = pageIndex,
                onClose = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refreshKey", System.currentTimeMillis())
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "addpage/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            CameraScreen(
                onBackClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refreshKey", System.currentTimeMillis())
                    navController.popBackStack()
                },
                addToDocumentId = documentId
            )
        }
        composable("camera") {
            CameraScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToPreview = { paths ->
                    val encoded = paths.joinToString("|") { Uri.encode(it) }
                    navController.navigate("preview/${Uri.encode(encoded)}")
                }
            )
        }
        composable(
            route = "preview/{imagePaths}",
            arguments = listOf(navArgument("imagePaths") { type = NavType.StringType })
        ) { backStackEntry ->
            val raw = Uri.decode(backStackEntry.arguments?.getString("imagePaths") ?: "")
            val imagePaths = raw.split("|").map { Uri.decode(it) }
            PreviewScreen(
                imagePaths = imagePaths,
                onBackClick = { navController.popBackStack() },
                onSaveClick = {
                    navController.navigate("files") {
                        popUpTo("files") { inclusive = true }
                    }
                }
            )
        }
    }
}
