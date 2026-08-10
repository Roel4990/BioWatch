package com.example.biowatch.presentation.screen.home

data class HomeUiState(
    val heartRate: Int? = null,
    val status: HomeStatus = HomeStatus.DISCONNECTED,
    val isWatchWorn: Boolean = true,
    val isTracking: Boolean = false
)

enum class HomeStatus {
    DISCONNECTED,
    CONNECTING,
    PREPARING,
    MEASURING,
    WATCH_NOT_WORN,
    NOT_SUPPORTED,
    PERMISSION_REQUIRED,
    ADJUST_WATCH,
    ERROR
}
