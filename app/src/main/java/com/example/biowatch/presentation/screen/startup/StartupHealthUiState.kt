package com.example.biowatch.presentation.screen.startup

data class StartupHealthUiState(
    val status: StartupHealthStatus = StartupHealthStatus.CHECKING,
    val modelVersion: String? = null,
    val errorMessage: String? = null
)

enum class StartupHealthStatus {
    CHECKING,
    READY,
    ERROR
}
