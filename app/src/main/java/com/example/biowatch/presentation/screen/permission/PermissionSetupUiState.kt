package com.example.biowatch.presentation.screen.permission

data class PermissionSetupUiState(
    val status: PermissionSetupStatus = PermissionSetupStatus.CHECKING,
    val missingPermissionLabels: List<String> = emptyList(),
    val message: String? = null
)

enum class PermissionSetupStatus {
    CHECKING,
    REQUIRED,
    REQUESTING,
    GRANTED,
    DENIED
}
