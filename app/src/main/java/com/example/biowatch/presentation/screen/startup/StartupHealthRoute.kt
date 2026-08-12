package com.example.biowatch.presentation.screen.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun StartupHealthRoute(
    onServerReady: () -> Unit,
    viewModel: StartupHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.status) {
        if (uiState.status == StartupHealthStatus.READY) {
            delay(SERVER_READY_DISPLAY_MILLIS)
            onServerReady()
        }
    }

    StartupHealthScreen(
        uiState = uiState,
        onRetry = viewModel::checkServer
    )
}

private const val SERVER_READY_DISPLAY_MILLIS = 1_000L
