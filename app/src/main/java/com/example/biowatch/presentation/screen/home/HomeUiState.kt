package com.example.biowatch.presentation.screen.home

import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.domain.model.ContinuousAnalysisPhase
import com.example.biowatch.domain.model.RhythmAnalysisResult
import com.example.biowatch.domain.model.StressAnalysisResult

data class HomeUiState(
    val heartRate: Int? = null,
    val status: HomeStatus = HomeStatus.DISCONNECTED,
    val isWatchWorn: Boolean = true,
    val isTracking: Boolean = false,
    val subjectId: String = "subject_01",
    val collectionLabel: CollectionLabel = CollectionLabel.NORMAL,
    val collectionPurpose: CollectionPurpose = CollectionPurpose.CALIBRATION,
    val isCollecting: Boolean = false,
    val collectionElapsedSeconds: Long = 0,
    val sampleCount: Long = 0,
    val samplingRateHz: Double = 0.0,
    val ppgSupported: Boolean? = null,
    val accelerometerSupported: Boolean? = null,
    val canShare: Boolean = false,
    val collectionMessage: String? = null,
    val analysisMessage: String? = null,
    val isAnalysisLoading: Boolean = false,
    val hasBaseline: Boolean = false,
    val baselineCreatedAt: String? = null,
    val baselineAverageHeartRate: Double? = null,
    val continuousAnalysisPhase: ContinuousAnalysisPhase = ContinuousAnalysisPhase.STOPPED,
    val continuousAnalysisProgress: Float = 0f,
    val continuousAnalysisElapsedSeconds: Long = 0,
    val continuousAnalysisTargetSeconds: Long = 65,
    val rhythmResult: RhythmAnalysisResult = RhythmAnalysisResult.WAITING,
    val abnormalProbability: Double? = null,
    val stressResult: StressAnalysisResult = StressAnalysisResult.WAITING,
    val acuteStressProbability: Double? = null,
    val lastAnalyzedAtMillis: Long? = null,
    val monitoringMessage: String? = null
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
