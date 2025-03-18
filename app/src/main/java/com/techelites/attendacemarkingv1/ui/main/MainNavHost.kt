package com.techelites.attendacemarkingv1.ui.main

import com.techelites.attendacemarkingv1.ui.main.home.HomeScreen
import com.techelites.attendacemarkingv1.ui.main.scanner.AttendanceScanner
import com.techelites.attendacemarkingv1.ui.main.scanner.IdCollectionScanner


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.techelites.attendacemarkingv1.ui.main.profile.ProfileScreen

enum class MainDestination {
    HOME,
    PROFILE,
    ATTENDANCE_SCANNER,
    ID_COLLECTION_SCANNER
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = MainDestination.HOME.name
    ) {
        composable(MainDestination.HOME.name) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAttendanceScanner = {
                    navController.navigate(MainDestination.ATTENDANCE_SCANNER.name)
                },
                onNavigateToIdCollectionScanner = {
                    navController.navigate(MainDestination.ID_COLLECTION_SCANNER.name)
                },
                onNavigateToProfile = {
                    navController.navigate(MainDestination.PROFILE.name)
                }
            )
        }

        composable(MainDestination.PROFILE.name) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(MainDestination.ATTENDANCE_SCANNER.name) {
            AttendanceScanner(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(MainDestination.ID_COLLECTION_SCANNER.name) {
            IdCollectionScanner(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}