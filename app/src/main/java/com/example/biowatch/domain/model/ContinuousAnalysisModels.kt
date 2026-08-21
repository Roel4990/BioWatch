package com.example.biowatch.domain.model

data class ContinuousAnalysisState(
    val phase: ContinuousAnalysisPhase = ContinuousAnalysisPhase.STOPPED,
    val subjectId: String? = null,
    val baselineAverageHeartRate: Double? = null,
    val elapsedSeconds: Long = 0,
    val targetSeconds: Long = 60,
    val sampleCount: Long = 0,
    val rhythmResult: RhythmAnalysisResult = RhythmAnalysisResult.WAITING,
    val abnormalProbability: Double? = null,
    val stressResult: StressAnalysisResult = StressAnalysisResult.WAITING,
    val acuteStressProbability: Double? = null,
    val fallResult: FallAnalysisResult = FallAnalysisResult.WAITING,
    val fallProbability: Double? = null,
    val fallEventTimeSec: Double? = null,
    val lastAnalyzedAtMillis: Long? = null,
    val message: String? = null
) {
    val progress: Float
        get() = (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
}

enum class ContinuousAnalysisPhase {
    STOPPED,
    WAITING_FOR_SENSOR,
    COLLECTING,
    ANALYZING,
    RESULT,
    RETRYING,
    BASELINE_REQUIRED
}

enum class RhythmAnalysisResult {
    WAITING,
    NORMAL,
    POSSIBLE_ABNORMAL,
    UNAVAILABLE
}

enum class StressAnalysisResult {
    WAITING,
    NORMAL,
    POSSIBLE_ACUTE_STRESS,
    UNAVAILABLE
}

enum class FallAnalysisResult {
    WAITING,
    NORMAL,
    FALL_CANDIDATE,
    UNAVAILABLE
}
