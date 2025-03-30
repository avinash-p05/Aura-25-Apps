package com.techelites.attendacemarkingv1.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.techelites.attendacemarkingv1.ui.main.home.HomeScreen
import com.techelites.attendacemarkingv1.ui.main.profile.ProfileScreen

enum class MainDestination {
    HOME,
    PROFILE
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    onNavigateToScanner: (String, String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = MainDestination.HOME.name
    ) {
        composable(MainDestination.HOME.name) {
            // Pass the shared ViewModel to HomeScreen
            HomeScreen(
                viewModel = viewModel,
                onNavigateToProfile = {
                    navController.navigate(MainDestination.PROFILE.name)
                }
            )
        }

        composable(MainDestination.PROFILE.name) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}