package com.example.biowatch.presentation.screen.calibration

data class CalibrationUiState(
    val status: CalibrationStatus = CalibrationStatus.PREPARING,
    val subjectId: String = "",
    val currentHeartRate: Int? = null,
    val elapsedSeconds: Long = 0,
    val targetSeconds: Long = 320,
    val sampleCount: Long = 0,
    val isWatchWorn: Boolean = true,
    val message: String? = null
) {
    val progress: Float
        get() = (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)

    val remainingSeconds: Long
        get() = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
}

enum class CalibrationStatus {
    PREPARING,
    MEASURING,
    UPLOADING,
    SUCCESS,
    ERROR
}
