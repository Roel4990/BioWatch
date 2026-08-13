package com.example.biowatch.presentation.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onOpenCollection: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.startContinuousAnalysis()
    }

    HomeScreen(
        uiState = uiState,
        onStartTracking = homeViewModel::startHeartRateTracking,
        onStopTracking = homeViewModel::stopHeartRateTracking,
        onOpenCollection = onOpenCollection
    )
}
