package com.example.biowatch.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.biowatch.presentation.screen.home.HomeRoute

private object Destination {
    const val HOME = "home"
}

@Composable
fun BioWatchNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Destination.HOME,
        modifier = modifier
    ) {
        composable(Destination.HOME) {
            HomeRoute()
        }
    }
}
