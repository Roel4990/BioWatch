package com.example.biowatch.presentation.screen.permission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PermissionSetupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionSetupUiState())
    val uiState: StateFlow<PermissionSetupUiState> = _uiState.asStateFlow()

    fun updateMissingPermissions(labels: List<String>) {
        _uiState.value = PermissionSetupUiState(
            status = if (labels.isEmpty()) {
                PermissionSetupStatus.GRANTED
            } else {
                PermissionSetupStatus.REQUIRED
            },
            missingPermissionLabels = labels
        )
    }

    fun startRequest(labels: List<String>) {
        _uiState.value = PermissionSetupUiState(
            status = PermissionSetupStatus.REQUESTING,
            missingPermissionLabels = labels
        )
    }

    fun onPermissionDenied(label: String, remainingLabels: List<String>) {
        _uiState.value = PermissionSetupUiState(
            status = PermissionSetupStatus.DENIED,
            missingPermissionLabels = remainingLabels,
            message = "$label 권한이 필요합니다."
        )
    }
}
