package com.example.biowatch.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.biowatch.presentation.screen.collection.CollectionRoute
import com.example.biowatch.presentation.screen.home.HomeRoute
import com.example.biowatch.presentation.screen.home.HomeViewModel

private object Destination {
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
        startDestination = Destination.HOME,
        modifier = modifier
    ) {
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
