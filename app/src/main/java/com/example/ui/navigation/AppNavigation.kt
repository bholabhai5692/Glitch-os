package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AdUnits
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.AppToolDestination
import com.example.ui.components.FloatingToolLauncher
import com.example.ui.screens.*
import com.example.ui.viewmodels.MainViewModel

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = NavRoute.Home.route) {
            composable(NavRoute.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = { navController.navigate(NavRoute.Create.route) },
                    onNavigateToLibrary = { navController.navigate(NavRoute.Library.route) },
                    onNavigateToSettings = { navController.navigate(NavRoute.Settings.route) },
                    onNavigateToKeyboardPlayground = { navController.navigate(NavRoute.KeyboardPlayground.route) },
                    onNavigateToVpnShield = { navController.navigate(NavRoute.VpnShield.route) },
                    onNavigateToRecovery = { navController.navigate(NavRoute.Recovery.route) },
                    onNavigateToCreatorAssistant = { navController.navigate(NavRoute.CreatorAssistant.route) },
                    onNavigateToLocation = { navController.navigate(NavRoute.Location.route) },
                    onNavigateToAdProtection = { navController.navigate(NavRoute.AdProtection.route) }
                )
            }
            composable(NavRoute.Create.route) {
                CreateVideoScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProgress = { jobId ->
                        navController.navigate(NavRoute.Progress.createRoute(jobId)) {
                            popUpTo(NavRoute.Home.route)
                        }
                    }
                )
            }
            composable(
                route = NavRoute.Progress.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { entry ->
                ProgressScreen(
                    jobId = entry.arguments?.getString("jobId").orEmpty(),
                    viewModel = viewModel,
                    onNavigateToPreview = { url, title ->
                        navController.navigate(NavRoute.Preview.createRoute(url, title))
                    },
                    onNavigateHome = {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(NavRoute.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = NavRoute.Preview.route,
                arguments = listOf(
                    navArgument("videoUrl") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { entry ->
                PreviewScreen(
                    videoUrl = entry.arguments?.getString("videoUrl").orEmpty(),
                    title = entry.arguments?.getString("title") ?: "Video preview",
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoute.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { url, title ->
                        navController.navigate(NavRoute.Preview.createRoute(url, title))
                    },
                    onNavigateToDetails = { id ->
                        navController.navigate(NavRoute.Details.createRoute(id))
                    }
                )
            }
            composable(
                route = NavRoute.Details.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { entry ->
                ProjectDetailsScreen(
                    projectId = entry.arguments?.getString("projectId").orEmpty(),
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { url, title ->
                        navController.navigate(NavRoute.Preview.createRoute(url, title))
                    }
                )
            }
            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogs = { navController.navigate(NavRoute.Logs.route) }
                )
            }
            composable(NavRoute.Logs.route) {
                LogsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoute.KeyboardPlayground.route) {
                KeyboardPlaygroundScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoute.VpnShield.route) {
                VpnShieldScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdProtection = { navController.navigate(NavRoute.AdProtection.route) }
                )
            }
            composable(NavRoute.AdProtection.route) {
                AdProtectionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoute.Recovery.route) {
                RecoverScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoute.Location.route) {
                LocationScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoute.CreatorAssistant.route) {
                ChatbotScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { url, title ->
                        navController.navigate(NavRoute.Preview.createRoute(url, title))
                    }
                )
            }
        }

        FloatingToolLauncher(
            destinations = listOf(
                AppToolDestination("Home", Icons.Default.Home, NavRoute.Home.route),
                AppToolDestination("Create video", Icons.Default.VideoSettings, NavRoute.Create.route),
                AppToolDestination("Video library", Icons.Default.VideoLibrary, NavRoute.Library.route),
                AppToolDestination("Keyboard tools", Icons.Default.Keyboard, NavRoute.KeyboardPlayground.route),
                AppToolDestination("VPN settings", Icons.Default.VpnKey, NavRoute.VpnShield.route),
                AppToolDestination("Ad protection", Icons.Default.AdUnits, NavRoute.AdProtection.route),
                AppToolDestination("Recover files", Icons.Default.Restore, NavRoute.Recovery.route),
                AppToolDestination("Location", Icons.Default.Place, NavRoute.Location.route),
                AppToolDestination("Creator assistant", Icons.Default.AutoAwesome, NavRoute.CreatorAssistant.route),
                AppToolDestination("Settings", Icons.Default.Settings, NavRoute.Settings.route),
                AppToolDestination("Logs", Icons.Default.Article, NavRoute.Logs.route)
            ),
            onNavigate = { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
        )
    }
}
