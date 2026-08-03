package com.example.biowatch.presentation.screen.home

data class HomeUiState(
    val heartRate: Int = 72,
    val status: String = "Connected",
    val steps: Int = 0
)
