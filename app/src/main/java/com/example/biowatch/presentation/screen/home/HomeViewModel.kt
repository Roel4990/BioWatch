package com.example.biowatch.presentation.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.domain.model.CollectionState
import com.example.biowatch.domain.model.ContinuousAnalysisPhase
import com.example.biowatch.domain.model.ContinuousAnalysisState
import com.example.biowatch.domain.repository.HealthRepository
import com.example.biowatch.data.network.AnalysisApiClient
import com.example.biowatch.data.storage.BaselinePreferences
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val analysisApiClient: AnalysisApiClient,
    private val baselinePreferences: BaselinePreferences
) : ViewModel() {

    private val permissionDenied = MutableStateFlow(false)
    private val subjectId = MutableStateFlow("subject_01")
    private val collectionLabel = MutableStateFlow(CollectionLabel.NORMAL)
    private val collectionPurpose = MutableStateFlow(CollectionPurpose.CALIBRATION)
    private val elapsedSeconds = MutableStateFlow(0L)
    private val analysisState = MutableStateFlow(AnalysisState())
    private var hasRestoredBaseline = false

    init {
        viewModelScope.launch {
            baselinePreferences.savedBaseline.collect { saved ->
                subjectId.value = saved.subjectId
                if (!hasRestoredBaseline) {
                    collectionPurpose.value = if (saved.baselineId == null) {
                        CollectionPurpose.CALIBRATION
                    } else {
                        CollectionPurpose.EVALUATION
                    }
                    hasRestoredBaseline = true
                }
                analysisState.value = analysisState.value.copy(
                    baselineId = saved.baselineId,
                    baselineSubjectId = saved.subjectId.takeIf { saved.baselineId != null },
                    baselineCreatedAt = saved.baselineCreatedAt,
                    baselineAverageHeartRate = saved.averageHeartRate
                )
            }
        }
    }
    private val collectionForm = combine(
        subjectId,
        collectionLabel,
        collectionPurpose,
        elapsedSeconds,
        analysisState
    ) { id, label, purpose, elapsed, analysis ->
        CollectionForm(id, label, purpose, elapsed, analysis)
    }

    private val healthSnapshot = combine(
        healthRepository.connectionState,
        healthRepository.heartRate,
        healthRepository.collectionState,
        healthRepository.continuousAnalysisState
    ) { connectionState, heartRate, collectionState, continuousAnalysisState ->
        HealthSnapshot(
            connectionState = connectionState,
            heartRate = heartRate,
            collectionState = collectionState,
            continuousAnalysisState = continuousAnalysisState
        )
    }

    val uiState = combine(
        healthSnapshot,
        permissionDenied,
        collectionForm
    ) { health, isPermissionDenied, form ->
        val continuous = health.continuousAnalysisState
        val continuousForSubject = continuous.takeIf {
            it.subjectId == form.subjectId
        } ?: ContinuousAnalysisState()
        HomeUiState(
            heartRate = health.heartRate,
            isWatchWorn =
                health.connectionState != HealthServiceConnectionState.WatchNotWorn,
            isTracking = !isPermissionDenied &&
                health.connectionState != HealthServiceConnectionState.Disconnected,
            status = if (isPermissionDenied) {
                HomeStatus.PERMISSION_REQUIRED
            } else {
                health.connectionState.toHomeStatus()
            },
            subjectId = form.subjectId,
            collectionLabel = form.label,
            collectionPurpose = form.purpose,
            isCollecting = health.collectionState.isCollecting,
            collectionElapsedSeconds = form.elapsedSeconds,
            sampleCount = health.collectionState.sampleCount,
            samplingRateHz = health.collectionState.samplingRateHz,
            ppgSupported = health.collectionState.ppgSupported,
            accelerometerSupported = health.collectionState.accelerometerSupported,
            canShare = health.collectionState.savedCsvPath != null,
            collectionMessage = health.collectionState.errorMessage,
            analysisMessage = form.analysis.message,
            isAnalysisLoading = form.analysis.isLoading,
            hasBaseline = form.analysis.baselineId != null,
            baselineCreatedAt = form.analysis.baselineCreatedAt,
            baselineAverageHeartRate = form.analysis.baselineAverageHeartRate,
            continuousAnalysisPhase = continuousForSubject.phase,
            continuousAnalysisProgress = when (continuousForSubject.phase) {
                ContinuousAnalysisPhase.ANALYZING,
                ContinuousAnalysisPhase.RESULT -> 1f
                else -> continuousForSubject.progress
            },
            continuousAnalysisElapsedSeconds = continuousForSubject.elapsedSeconds,
            continuousAnalysisTargetSeconds = continuousForSubject.targetSeconds,
            rhythmResult = continuousForSubject.rhythmResult,
            abnormalProbability = continuousForSubject.abnormalProbability,
            stressResult = continuousForSubject.stressResult,
            acuteStressProbability = continuousForSubject.acuteStressProbability,
            fallResult = continuousForSubject.fallResult,
            fallProbability = continuousForSubject.fallProbability,
            fallEventTimeSec = continuousForSubject.fallEventTimeSec,
            lastAnalyzedAtMillis = continuousForSubject.lastAnalyzedAtMillis,
            monitoringMessage = continuousForSubject.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun startHeartRateTracking() {
        permissionDenied.value = false
        healthRepository.connect()
    }

    fun onPermissionDenied() {
        Log.w(TAG, "Required foreground or background health permission denied")
        permissionDenied.value = true
        healthRepository.disconnect()
    }

    fun stopHeartRateTracking() {
        healthRepository.disconnect()
    }

    fun updateSubjectId(value: String) {
        val filtered = value.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(MAX_SUBJECT_ID_LENGTH)
        subjectId.value = filtered
        val current = analysisState.value
        if (current.baselineSubjectId != null && current.baselineSubjectId != filtered) {
            analysisState.value = AnalysisState(
                message = "대상자 ID가 변경되어 개인 기준을 다시 생성해야 합니다."
            )
            collectionPurpose.value = CollectionPurpose.CALIBRATION
        }
        viewModelScope.launch { baselinePreferences.saveSubjectId(filtered) }
    }

    fun selectCollectionLabel(value: CollectionLabel) {
        collectionLabel.value = value
    }

    fun selectCollectionPurpose(value: CollectionPurpose) {
        collectionPurpose.value = value
    }

    fun startCollection() {
        val normalizedSubjectId = subjectId.value.trim()
        if (normalizedSubjectId.isEmpty()) return
        healthRepository.startCollection(
            CollectionConfig(
                subjectId = normalizedSubjectId,
                state = collectionLabel.value,
                purpose = collectionPurpose.value
            )
        )
        startElapsedTimer()
    }

    fun stopCollection() {
        healthRepository.stopCollection()
    }

    fun shareSavedFiles() {
        healthRepository.shareSavedFiles()
    }

    fun startContinuousAnalysis() {
        permissionDenied.value = false
        healthRepository.startContinuousAnalysis()
    }

    fun stopContinuousAnalysis() {
        healthRepository.stopContinuousAnalysis()
    }

    fun checkAnalysisServer() = runAnalysis {
        val health = analysisApiClient.health()
        val fallModelStatus = if (health.fallModelLoaded) {
            " · 낙상 ${health.fallModelVersion.ifBlank { "준비됨" }}"
        } else {
            " · 낙상 모델 미준비"
        }
        analysisState.value = analysisState.value.copy(
            isLoading = false,
            message = if (health.status == "ok" && health.modelLoaded) {
                "서버 연결 성공 · ${health.modelVersion}$fallModelStatus"
            } else {
                "서버는 연결됐지만 모델이 준비되지 않았습니다."
            }
        )
    }

    fun uploadSavedData() = runAnalysis {
        val state = healthRepository.collectionState.value
        val csvFile = state.savedCsvPath?.let(::File)
            ?: error("먼저 센서 데이터를 수집하고 저장해 주세요.")
        val id = subjectId.value.trim()

        if (collectionPurpose.value == CollectionPurpose.CALIBRATION) {
            require(collectionLabel.value == CollectionLabel.NORMAL) {
                "기준 생성에는 정상 상태 데이터만 사용할 수 있습니다."
            }
            require(elapsedSeconds.value >= CALIBRATION_MIN_SECONDS) {
                "기준 데이터는 5분 이상 수집해 주세요."
            }
            val result = analysisApiClient.calibrate(id, csvFile)
            baselinePreferences.save(id, result.baselineId, result.createdAt)
            val filesDeleted = healthRepository.deleteSavedFiles()
            analysisState.value = AnalysisState(
                baselineId = result.baselineId,
                baselineSubjectId = id,
                baselineCreatedAt = result.createdAt,
                message = "개인 기준 생성 완료 · 유효 구간 ${result.validWindowCount}개" +
                    deletionMessage(filesDeleted)
            )
        } else {
            require(elapsedSeconds.value >= PREDICTION_MIN_SECONDS) {
                "평가 데이터는 60초 수집해 주세요."
            }
            val current = analysisState.value
            val baselineId = current.baselineId
                ?: error("먼저 정상 5분 데이터로 개인 기준을 생성해 주세요.")
            require(current.baselineSubjectId == id) {
                "개인 기준을 생성한 대상자 ID와 현재 ID가 다릅니다."
            }
            val result = analysisApiClient.predict(id, baselineId, csvFile)
            val filesDeleted = healthRepository.deleteSavedFiles()
            if (result.result == "unavailable" && result.reason.isMissingBaselineReason()) {
                baselinePreferences.clearBaseline()
                analysisState.value = AnalysisState(
                    message = "서버에서 개인 기준을 찾을 수 없습니다. 정상 상태에서 5분간 다시 교정해 주세요." +
                        deletionMessage(filesDeleted)
                )
                return@runAnalysis
            }
            val resultText = when (result.result) {
                "normal" -> "정상 가능성"
                "possible_abnormal" -> "이상 가능성"
                "unavailable" -> "분석 불가"
                else -> "알 수 없는 결과"
            }
            val probability = result.abnormalProbability?.let {
                " · 이상 확률 ${String.format("%.1f", it * 100)}%"
            }.orEmpty()
            analysisState.value = current.copy(
                isLoading = false,
                message = "$resultText$probability · 유효 구간 ${result.validWindowCount}개" +
                    deletionMessage(filesDeleted)
            )
        }
    }

    fun uploadStressPrediction() = runAnalysis {
        require(collectionPurpose.value == CollectionPurpose.EVALUATION) {
            "평가 데이터를 선택해 주세요."
        }
        require(elapsedSeconds.value >= PREDICTION_MIN_SECONDS) {
            "평가 데이터는 60초 수집해 주세요."
        }
        val state = healthRepository.collectionState.value
        val csvFile = state.savedCsvPath?.let(::File)
            ?: error("먼저 평가 데이터를 수집하고 저장해 주세요.")
        val id = subjectId.value.trim()
        val current = analysisState.value
        val baselineId = current.baselineId
            ?: error("먼저 정상 5분 데이터로 개인 기준을 생성해 주세요.")
        require(current.baselineSubjectId == id) {
            "개인 기준을 생성한 대상자 ID와 현재 ID가 다릅니다."
        }

        val result = analysisApiClient.predictStress(id, baselineId, csvFile)
        val filesDeleted = healthRepository.deleteSavedFiles()
        if (result.result == "unavailable" && result.reason.isMissingBaselineReason()) {
            baselinePreferences.clearBaseline()
            analysisState.value = AnalysisState(
                message = "서버에서 개인 기준을 찾을 수 없습니다. 정상 상태에서 5분간 다시 교정해 주세요." +
                    deletionMessage(filesDeleted)
            )
            return@runAnalysis
        }

        val resultText = when (result.result) {
            "normal" -> "급성 스트레스 정상 범위"
            "possible_acute_stress" -> "급성 스트레스 가능성"
            "unavailable" -> "급성 스트레스 분석 불가"
            else -> "알 수 없는 스트레스 결과"
        }
        val probability = result.acuteStressProbability?.let {
            " · 가능성 ${String.format("%.1f", it * 100)}%"
        }.orEmpty()
        analysisState.value = current.copy(
            isLoading = false,
            message = "$resultText$probability · 유효 구간 ${result.validWindowCount}개" +
                deletionMessage(filesDeleted)
        )
    }

    fun uploadFallPrediction() = runAnalysis(
        loadingMessage = "낙상 가능성을 분석하고 있습니다."
    ) {
        require(collectionPurpose.value == CollectionPurpose.EVALUATION) {
            "평가 데이터를 선택해 주세요."
        }
        require(elapsedSeconds.value >= PREDICTION_MIN_SECONDS) {
            "평가 데이터는 60초 수집해 주세요."
        }
        val state = healthRepository.collectionState.value
        val csvFile = state.savedCsvPath?.let(::File)
            ?: error("먼저 평가 데이터를 수집하고 저장해 주세요.")
        val id = subjectId.value.trim()
        val health = analysisApiClient.health()
        require(health.status == "ok" && health.fallModelLoaded) {
            "낙상 분석 모델이 준비되지 않았습니다."
        }

        val result = analysisApiClient.predictFall(id, csvFile)
        val filesDeleted = healthRepository.deleteSavedFiles()
        val resultText = when (result.result) {
            "normal" -> "낙상 의심 움직임 없음"
            "fall_candidate" -> "낙상 의심 움직임 감지"
            "unavailable" -> "낙상 분석 불가"
            else -> "알 수 없는 낙상 분석 결과"
        }
        val messageLines = buildList {
            add(resultText)
            result.fallProbability?.let { probability ->
                add(
                    "낙상 가능성: ${String.format(Locale.US, "%.2f", probability * 100)}%"
                )
            }
            if (result.result == "fall_candidate") {
                result.eventTimeSec?.let { eventTimeSec ->
                    add(
                        "감지 시점: 측정 시작 후 " +
                            "${String.format(Locale.US, "%.2f", eventTimeSec)}초"
                    )
                }
            }
            if (result.result == "unavailable") {
                add("사유: ${result.reason.toFallUnavailableMessage()}")
            }
            add(if (filesDeleted) "전송 파일 삭제 완료" else "저장 파일 삭제 실패")
        }
        analysisState.value = analysisState.value.copy(
            isLoading = false,
            message = messageLines.joinToString("\n")
        )
    }

    private fun runAnalysis(
        loadingMessage: String = "서버 요청 중...",
        block: suspend () -> Unit
    ) {
        if (analysisState.value.isLoading) return
        analysisState.value = analysisState.value.copy(
            isLoading = true,
            message = loadingMessage
        )
        viewModelScope.launch {
            runCatching { block() }.onFailure { error ->
                Log.e(TAG, "Analysis API request failed", error)
                analysisState.value = analysisState.value.copy(
                    isLoading = false,
                    message = error.toAnalysisErrorMessage()
                )
            }
        }
    }

    private fun startElapsedTimer() {
        viewModelScope.launch {
            elapsedSeconds.value = 0
            while (healthRepository.collectionState.value.isCollecting) {
                val start = healthRepository.collectionState.value.startTimestampMillis
                elapsedSeconds.value = if (start == null) 0 else {
                    ((System.currentTimeMillis() - start) / 1_000).coerceAtLeast(0)
                }
                delay(1_000)
            }
        }
    }

    private fun String?.isMissingBaselineReason(): Boolean {
        val normalized = this?.lowercase().orEmpty()
        if (!normalized.contains("baseline")) return false
        return MISSING_BASELINE_MARKERS.any(normalized::contains)
    }

    private fun String?.toFallUnavailableMessage(): String {
        if (isNullOrBlank()) return "유효한 가속도 데이터를 확인해 주세요."
        val normalized = lowercase()
        return when {
            contains("필수 열") || normalized.contains("required column") ||
                normalized.contains("missing column") || normalized.contains("acc_") ->
                "가속도 필수 데이터가 누락되었습니다."
            contains("길이") || normalized.contains("duration") ->
                "분석할 가속도 데이터 길이가 부족합니다."
            contains("샘플") || normalized.contains("sample") ||
                normalized.contains("sampling") ->
                "유효한 가속도 샘플이 부족합니다."
            contains("형식") || normalized.contains("format") ->
                "가속도 데이터 형식을 확인해 주세요."
            else -> this
        }
    }

    private fun Throwable.toAnalysisErrorMessage(): String = when (this) {
        is SocketTimeoutException -> "서버 응답 시간이 초과되었습니다. 다시 시도해 주세요."
        is ConnectException,
        is UnknownHostException -> "FastAPI 서버에 연결할 수 없습니다."
        else -> message ?: "서버 요청에 실패했습니다."
    }

    private fun deletionMessage(filesDeleted: Boolean): String =
        if (filesDeleted) " · 전송 파일 삭제 완료" else " · 저장 파일 삭제 실패"

    private fun HealthServiceConnectionState.toHomeStatus(): HomeStatus = when (this) {
        HealthServiceConnectionState.Disconnected -> HomeStatus.DISCONNECTED
        HealthServiceConnectionState.Connecting -> HomeStatus.CONNECTING
        HealthServiceConnectionState.WaitingForHeartRate -> HomeStatus.PREPARING
        HealthServiceConnectionState.Connected -> HomeStatus.MEASURING
        HealthServiceConnectionState.WatchNotWorn -> HomeStatus.WATCH_NOT_WORN
        HealthServiceConnectionState.HeartRateUnsupported -> HomeStatus.NOT_SUPPORTED
        is HealthServiceConnectionState.MeasurementUnavailable -> HomeStatus.ADJUST_WATCH
        is HealthServiceConnectionState.Error -> HomeStatus.ERROR
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val MAX_SUBJECT_ID_LENGTH = 32
        const val CALIBRATION_MIN_SECONDS = 300L
        const val PREDICTION_MIN_SECONDS = 55L
        val MISSING_BASELINE_MARKERS = listOf(
            "not found",
            "missing",
            "unknown",
            "does not exist",
            "invalid"
        )
    }

    private data class CollectionForm(
        val subjectId: String,
        val label: CollectionLabel,
        val purpose: CollectionPurpose,
        val elapsedSeconds: Long,
        val analysis: AnalysisState
    )

    private data class HealthSnapshot(
        val connectionState: HealthServiceConnectionState,
        val heartRate: Int?,
        val collectionState: CollectionState,
        val continuousAnalysisState: ContinuousAnalysisState
    )

    private data class AnalysisState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val baselineId: String? = null,
        val baselineSubjectId: String? = null,
        val baselineCreatedAt: String? = null,
        val baselineAverageHeartRate: Double? = null
    )
}
