package com.example.biowatch.presentation.screen.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.biowatch.presentation.screen.home.HomeViewModel

@Composable
fun CollectionRoute(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()

    CollectionScreen(
        uiState = uiState,
        onBack = onBack,
        onSubjectIdChange = homeViewModel::updateSubjectId,
        onCollectionLabelChange = homeViewModel::selectCollectionLabel,
        onCollectionPurposeChange = homeViewModel::selectCollectionPurpose,
        onStartCollection = homeViewModel::startCollection,
        onStopCollection = homeViewModel::stopCollection,
        onShareFiles = homeViewModel::shareSavedFiles,
        onCheckServer = homeViewModel::checkAnalysisServer,
        onUploadSavedData = homeViewModel::uploadSavedData,
        onUploadStressPrediction = homeViewModel::uploadStressPrediction,
        onUploadFallPrediction = homeViewModel::uploadFallPrediction
    )
}
