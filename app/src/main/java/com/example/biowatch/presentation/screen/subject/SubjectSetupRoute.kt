package com.example.biowatch.presentation.screen.subject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SubjectSetupRoute(
    onUseExistingBaseline: () -> Unit,
    onCreateBaseline: () -> Unit,
    viewModel: SubjectSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.destinations.collect { destination ->
            when (destination) {
                SubjectSetupDestination.USE_EXISTING_BASELINE -> onUseExistingBaseline()
                SubjectSetupDestination.CREATE_BASELINE -> onCreateBaseline()
            }
        }
    }

    SubjectSetupScreen(
        uiState = uiState,
        onSubjectIdChange = viewModel::updateSubjectId,
        onContinue = viewModel::continueSetup,
        onUseExistingBaseline = viewModel::useExistingBaseline,
        onCreateBaseline = viewModel::createNewBaseline,
        onDismissBaselineChoice = viewModel::dismissBaselineChoice
    )
}
