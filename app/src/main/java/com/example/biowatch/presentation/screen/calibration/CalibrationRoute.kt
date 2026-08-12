package com.example.biowatch.presentation.screen.calibration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CalibrationRoute(
    onCalibrationComplete: () -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCalibrationComplete() }
    }

    CalibrationScreen(
        uiState = uiState,
        onRetry = viewModel::retry
    )
}
