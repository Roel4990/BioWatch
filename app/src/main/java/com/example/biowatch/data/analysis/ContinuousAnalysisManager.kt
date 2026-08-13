package com.example.biowatch.data.analysis

import android.util.Log
import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.data.network.AnalysisApiClient
import com.example.biowatch.data.network.PredictionResult
import com.example.biowatch.data.network.StressPredictionResult
import com.example.biowatch.data.storage.BaselinePreferences
import com.example.biowatch.data.storage.SavedBaseline
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.domain.model.ContinuousAnalysisPhase
import com.example.biowatch.domain.model.ContinuousAnalysisState
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.RhythmAnalysisResult
import com.example.biowatch.domain.model.StressAnalysisResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class ContinuousAnalysisManager @Inject constructor(
    private val healthDataSource: HealthDataSource,
    private val analysisApiClient: AnalysisApiClient,
    private val baselinePreferences: BaselinePreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(ContinuousAnalysisState())
    val state: StateFlow<ContinuousAnalysisState> = _state.asStateFlow()

    private var analysisJob: Job? = null

    @Synchronized
    fun start() {
        if (analysisJob?.isActive == true) return
        analysisJob = scope.launch { runAnalysisLoop() }
    }

    @Synchronized
    fun stop() {
        analysisJob?.cancel()
        analysisJob = null
        cleanupCollection()
        _state.value = _state.value.copy(
            phase = ContinuousAnalysisPhase.STOPPED,
            elapsedSeconds = 0,
            sampleCount = 0,
            message = null
        )
    }

    private suspend fun runAnalysisLoop() {
        while (currentCoroutineContext().isActive) {
            try {
                if (!runAnalysisCycle()) return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.e(TAG, "Continuous analysis cycle failed", error)
                cleanupCollection()
                _state.value = _state.value.copy(
                    phase = ContinuousAnalysisPhase.RETRYING,
                    elapsedSeconds = 0,
                    sampleCount = 0,
                    message = error.message ?: "연속 분석 중 오류가 발생했습니다."
                )
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    private suspend fun runAnalysisCycle(): Boolean {
        val baseline = baselinePreferences.savedBaseline.first()
        if (baseline.baselineId == null) {
            _state.value = ContinuousAnalysisState(
                phase = ContinuousAnalysisPhase.BASELINE_REQUIRED,
                subjectId = baseline.subjectId,
                message = "개인 기준을 다시 생성해 주세요."
            )
            return false
        }

        _state.value = _state.value.copy(
            subjectId = baseline.subjectId,
            baselineAverageHeartRate = baseline.averageHeartRate
        )
        waitForSensor()
        val csvFile = collectWindow(baseline) ?: return true
        return analyzeWithRetry(baseline, csvFile)
    }

    private suspend fun waitForSensor() {
        while (currentCoroutineContext().isActive) {
            when (val connection = healthDataSource.connectionState.value) {
                HealthServiceConnectionState.Connected -> return
                HealthServiceConnectionState.WatchNotWorn -> {
                    _state.value = _state.value.copy(
                        phase = ContinuousAnalysisPhase.WAITING_FOR_SENSOR,
                        message = "워치를 착용하면 다음 분석을 시작합니다."
                    )
                }
                is HealthServiceConnectionState.Error -> {
                    _state.value = _state.value.copy(
                        phase = ContinuousAnalysisPhase.RETRYING,
                        message = connection.message
                    )
                }
                else -> {
                    _state.value = _state.value.copy(
                        phase = ContinuousAnalysisPhase.WAITING_FOR_SENSOR,
                        message = "센서 연결을 기다리고 있습니다."
                    )
                }
            }
            delay(STATE_CHECK_INTERVAL_MILLIS)
        }
    }

    private suspend fun collectWindow(baseline: SavedBaseline): File? {
        healthDataSource.deleteSavedFiles()
        healthDataSource.startCollection(
            CollectionConfig(
                subjectId = baseline.subjectId,
                state = CollectionLabel.UNKNOWN,
                purpose = CollectionPurpose.EVALUATION
            )
        )
        if (!healthDataSource.collectionState.value.isCollecting) {
            _state.value = _state.value.copy(
                phase = ContinuousAnalysisPhase.RETRYING,
                message = healthDataSource.collectionState.value.errorMessage
                    ?: "평가 데이터 수집을 시작하지 못했습니다."
            )
            delay(RETRY_DELAY_MILLIS)
            return null
        }

        while (currentCoroutineContext().isActive) {
            if (healthDataSource.connectionState.value ==
                HealthServiceConnectionState.WatchNotWorn
            ) {
                cleanupCollection()
                _state.value = _state.value.copy(
                    phase = ContinuousAnalysisPhase.WAITING_FOR_SENSOR,
                    elapsedSeconds = 0,
                    sampleCount = 0,
                    message = "워치 미착용으로 현재 구간을 취소했습니다."
                )
                return null
            }

            val collection = healthDataSource.collectionState.value
            if (!collection.isCollecting) {
                healthDataSource.deleteSavedFiles()
                _state.value = _state.value.copy(
                    phase = ContinuousAnalysisPhase.RETRYING,
                    elapsedSeconds = 0,
                    sampleCount = 0,
                    message = collection.errorMessage ?: "센서 수집이 중단됐습니다."
                )
                delay(RETRY_DELAY_MILLIS)
                return null
            }
            val start = collection.startTimestampMillis ?: System.currentTimeMillis()
            val elapsed = ((System.currentTimeMillis() - start) / 1_000).coerceAtLeast(0)
            _state.value = _state.value.copy(
                phase = ContinuousAnalysisPhase.COLLECTING,
                elapsedSeconds = elapsed.coerceAtMost(ANALYSIS_WINDOW_SECONDS),
                sampleCount = collection.sampleCount,
                message = null
            )
            if (elapsed >= ANALYSIS_WINDOW_SECONDS) break
            delay(STATE_CHECK_INTERVAL_MILLIS)
        }

        healthDataSource.stopCollection()
        val stopped = healthDataSource.collectionState.value
        return stopped.savedCsvPath?.let(::File)?.takeIf(File::exists)
            ?: error("수집된 평가 CSV를 찾을 수 없습니다.")
    }

    private suspend fun analyzeWithRetry(baseline: SavedBaseline, csvFile: File): Boolean {
        while (currentCoroutineContext().isActive) {
            _state.value = _state.value.copy(
                phase = ContinuousAnalysisPhase.ANALYZING,
                elapsedSeconds = ANALYSIS_WINDOW_SECONDS,
                message = "부정맥과 급성 스트레스 분석 중"
            )
            val results = runCatching { requestBothPredictions(baseline, csvFile) }
            if (results.isFailure) {
                Log.w(
                    TAG,
                    "Analysis API request failed; retrying saved window",
                    results.exceptionOrNull()
                )
                _state.value = _state.value.copy(
                    phase = ContinuousAnalysisPhase.RETRYING,
                    message = "서버 요청 실패 · 같은 데이터를 다시 전송합니다."
                )
                delay(RETRY_DELAY_MILLIS)
                continue
            }

            val (rhythm, stress) = results.getOrThrow()
            if (rhythm.hasMissingBaseline() || stress.hasMissingBaseline()) {
                baselinePreferences.clearBaseline()
                deleteFilesUntilSuccessful()
                _state.value = _state.value.copy(
                    phase = ContinuousAnalysisPhase.BASELINE_REQUIRED,
                    elapsedSeconds = 0,
                    sampleCount = 0,
                    message = "서버에서 개인 기준을 찾을 수 없습니다. 다시 측정해 주세요."
                )
                return false
            }

            deleteFilesUntilSuccessful()
            _state.value = _state.value.copy(
                phase = ContinuousAnalysisPhase.RESULT,
                elapsedSeconds = 0,
                sampleCount = 0,
                rhythmResult = rhythm.result.toRhythmResult(),
                abnormalProbability = rhythm.abnormalProbability,
                stressResult = stress.result.toStressResult(),
                acuteStressProbability = stress.acuteStressProbability,
                lastAnalyzedAtMillis = System.currentTimeMillis(),
                message = null
            )
            delay(RESULT_DISPLAY_MILLIS)
            return true
        }
        return false
    }

    private suspend fun requestBothPredictions(
        baseline: SavedBaseline,
        csvFile: File
    ): Pair<PredictionResult, StressPredictionResult> = coroutineScope {
        val rhythm = async {
            analysisApiClient.predict(
                subjectId = baseline.subjectId,
                baselineId = requireNotNull(baseline.baselineId),
                csvFile = csvFile
            )
        }
        val stress = async {
            analysisApiClient.predictStress(
                subjectId = baseline.subjectId,
                baselineId = requireNotNull(baseline.baselineId),
                csvFile = csvFile
            )
        }
        rhythm.await() to stress.await()
    }

    private suspend fun deleteFilesUntilSuccessful() {
        while (currentCoroutineContext().isActive && !healthDataSource.deleteSavedFiles()) {
            _state.value = _state.value.copy(
                phase = ContinuousAnalysisPhase.RETRYING,
                message = "임시 측정 파일을 정리하고 있습니다."
            )
            delay(RETRY_DELAY_MILLIS)
        }
    }

    private fun cleanupCollection() {
        if (healthDataSource.collectionState.value.isCollecting) {
            healthDataSource.stopCollection()
        }
        healthDataSource.deleteSavedFiles()
    }

    private fun PredictionResult.hasMissingBaseline(): Boolean =
        result == "unavailable" && reason.isMissingBaselineReason()

    private fun StressPredictionResult.hasMissingBaseline(): Boolean =
        result == "unavailable" && reason.isMissingBaselineReason()

    private fun String?.isMissingBaselineReason(): Boolean {
        val normalized = this?.lowercase().orEmpty()
        if (!normalized.contains("baseline")) return false
        return MISSING_BASELINE_MARKERS.any(normalized::contains)
    }

    private fun String.toRhythmResult(): RhythmAnalysisResult = when (this) {
        "normal" -> RhythmAnalysisResult.NORMAL
        "possible_abnormal" -> RhythmAnalysisResult.POSSIBLE_ABNORMAL
        else -> RhythmAnalysisResult.UNAVAILABLE
    }

    private fun String.toStressResult(): StressAnalysisResult = when (this) {
        "normal" -> StressAnalysisResult.NORMAL
        "possible_acute_stress" -> StressAnalysisResult.POSSIBLE_ACUTE_STRESS
        else -> StressAnalysisResult.UNAVAILABLE
    }

    private companion object {
        const val TAG = "ContinuousAnalysis"
        const val ANALYSIS_WINDOW_SECONDS = 65L
        const val STATE_CHECK_INTERVAL_MILLIS = 1_000L
        const val RETRY_DELAY_MILLIS = 10_000L
        const val RESULT_DISPLAY_MILLIS = 2_000L
        val MISSING_BASELINE_MARKERS = listOf(
            "not found",
            "missing",
            "unknown",
            "does not exist",
            "invalid"
        )
    }
}
