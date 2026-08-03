package com.example.biowatch.presentation.screen.home

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    HomeScreen(uiState = homeViewModel.uiState)
}
