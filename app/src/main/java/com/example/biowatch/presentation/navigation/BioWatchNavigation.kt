package com.example.biowatch.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.biowatch.presentation.screen.collection.CollectionRoute
import com.example.biowatch.presentation.screen.calibration.CalibrationRoute
import com.example.biowatch.presentation.screen.home.HomeRoute
import com.example.biowatch.presentation.screen.home.HomeViewModel
import com.example.biowatch.presentation.screen.startup.StartupHealthRoute
import com.example.biowatch.presentation.screen.subject.SubjectSetupRoute

private object Destination {
    const val STARTUP = "startup"
    const val SUBJECT = "subject"
    const val CALIBRATION = "calibration"
    const val HOME = "home"
    const val COLLECTION = "collection"
}

@Composable
fun BioWatchNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberSwipeDismissableNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Destination.STARTUP,
        modifier = modifier
    ) {
        composable(Destination.STARTUP) {
            StartupHealthRoute(
                onServerReady = {
                    navController.navigate(Destination.SUBJECT) {
                        popUpTo(Destination.STARTUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.SUBJECT) {
            SubjectSetupRoute(
                onUseExistingBaseline = {
                    navController.navigate(Destination.HOME) {
                        popUpTo(Destination.SUBJECT) { inclusive = true }
                    }
                },
                onCreateBaseline = {
                    navController.navigate(Destination.CALIBRATION) {
                        popUpTo(Destination.SUBJECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.CALIBRATION) {
            CalibrationRoute(
                onCalibrationComplete = {
                    navController.navigate(Destination.HOME) {
                        popUpTo(Destination.CALIBRATION) { inclusive = true }
                    }
                }
            )
        }
        composable(Destination.HOME) {
            HomeRoute(
                homeViewModel = homeViewModel,
                onOpenCollection = { navController.navigate(Destination.COLLECTION) }
            )
        }
        composable(Destination.COLLECTION) {
            CollectionRoute(
                homeViewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
