package com.example.biowatch.presentation.screen.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.data.network.AnalysisApiClient
import com.example.biowatch.data.storage.BaselinePreferences
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val analysisApiClient: AnalysisApiClient,
    private val baselinePreferences: BaselinePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private val completedChannel = Channel<Unit>(Channel.BUFFERED)
    val completed = completedChannel.receiveAsFlow()

    private var calibrationJob: Job? = null

    init {
        observeSensorState()
        startCalibration()
    }

    fun retry() {
        if (calibrationJob?.isActive == true) return
        val savedCsv = healthRepository.collectionState.value.savedCsvPath?.let(::File)
        if (savedCsv?.exists() == true && _uiState.value.elapsedSeconds >= MIN_UPLOAD_SECONDS) {
            calibrationJob = viewModelScope.launch {
                runCatching { uploadCalibration(savedCsv) }.onFailure(::handleFailure)
            }
        } else {
            healthRepository.deleteSavedFiles()
            startCalibration()
        }
    }

    private fun startCalibration() {
        calibrationJob = viewModelScope.launch {
            runCatching {
                val saved = baselinePreferences.savedBaseline.first()
                _uiState.value = CalibrationUiState(subjectId = saved.subjectId)
                healthRepository.connect()

                val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MILLIS) {
                    healthRepository.connectionState
                        .filter { it == HealthServiceConnectionState.Connected }
                        .first()
                }
                check(connected != null) {
                    "센서에 연결할 수 없습니다. 권한과 워치 착용 상태를 확인해 주세요."
                }

                healthRepository.startCollection(
                    CollectionConfig(
                        subjectId = saved.subjectId,
                        state = CollectionLabel.NORMAL,
                        purpose = CollectionPurpose.CALIBRATION
                    )
                )
                check(healthRepository.collectionState.value.isCollecting) {
                    healthRepository.collectionState.value.errorMessage
                        ?: "기준 측정을 시작할 수 없습니다."
                }

                collectUntilTarget()
                healthRepository.stopCollection()
                val csvFile = healthRepository.collectionState.value.savedCsvPath?.let(::File)
                    ?: error("측정된 기준 CSV를 찾을 수 없습니다.")
                uploadCalibration(csvFile)
            }.onFailure(::handleFailure)
        }
    }

    private suspend fun collectUntilTarget() {
        while (currentCoroutineContext().isActive &&
            healthRepository.collectionState.value.isCollecting
        ) {
            val start = healthRepository.collectionState.value.startTimestampMillis
            val elapsed = if (start == null) 0 else {
                ((System.currentTimeMillis() - start) / 1_000).coerceAtLeast(0)
            }
            _uiState.value = _uiState.value.copy(
                status = CalibrationStatus.MEASURING,
                elapsedSeconds = elapsed.coerceAtMost(CALIBRATION_SECONDS),
                message = null
            )
            if (elapsed >= CALIBRATION_SECONDS) return
            delay(TIMER_INTERVAL_MILLIS)
        }
        error(healthRepository.collectionState.value.errorMessage
            ?: "기준 측정이 예기치 않게 중단됐습니다.")
    }

    private suspend fun uploadCalibration(csvFile: File) {
        _uiState.value = _uiState.value.copy(
            status = CalibrationStatus.UPLOADING,
            message = null
        )
        val averageHeartRate = calculateAverageHeartRate(csvFile)
        val subjectId = _uiState.value.subjectId
        val result = analysisApiClient.calibrate(subjectId, csvFile)
        baselinePreferences.save(
            subjectId = subjectId,
            baselineId = result.baselineId,
            createdAt = result.createdAt,
            averageHeartRate = averageHeartRate
        )
        check(healthRepository.deleteSavedFiles()) {
            "기준은 생성됐지만 임시 측정 파일을 삭제하지 못했습니다."
        }
        _uiState.value = _uiState.value.copy(
            status = CalibrationStatus.SUCCESS,
            message = "개인 기준 생성 완료"
        )
        delay(SUCCESS_DISPLAY_MILLIS)
        completedChannel.send(Unit)
    }

    private suspend fun calculateAverageHeartRate(csvFile: File): Double =
        withContext(Dispatchers.IO) {
            var total = 0.0
            var count = 0L
            csvFile.useLines { lines ->
                lines.drop(1).forEach { line ->
                    line.split(',').getOrNull(1)?.toDoubleOrNull()?.let { heartRate ->
                        if (heartRate > 0) {
                            total += heartRate
                            count++
                        }
                    }
                }
            }
            check(count > 0) { "평균 심박수를 계산할 유효한 데이터가 없습니다." }
            total / count
        }

    private fun observeSensorState() {
        viewModelScope.launch {
            combine(
                healthRepository.heartRate,
                healthRepository.connectionState,
                healthRepository.collectionState
            ) { heartRate, connectionState, collectionState ->
                Triple(heartRate, connectionState, collectionState)
            }.collect { (heartRate, connectionState, collectionState) ->
                _uiState.value = _uiState.value.copy(
                    currentHeartRate = heartRate,
                    sampleCount = collectionState.sampleCount,
                    isWatchWorn = connectionState != HealthServiceConnectionState.WatchNotWorn
                )
            }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) return
        if (healthRepository.collectionState.value.isCollecting) {
            healthRepository.stopCollection()
        }
        _uiState.value = _uiState.value.copy(
            status = CalibrationStatus.ERROR,
            message = error.message ?: "개인 기준 측정에 실패했습니다."
        )
    }

    override fun onCleared() {
        if (healthRepository.collectionState.value.isCollecting) {
            healthRepository.stopCollection()
            healthRepository.deleteSavedFiles()
        }
        super.onCleared()
    }

    private companion object {
        const val CALIBRATION_SECONDS = 320L
        const val MIN_UPLOAD_SECONDS = 320L
        const val TIMER_INTERVAL_MILLIS = 1_000L
        const val CONNECTION_TIMEOUT_MILLIS = 30_000L
        const val SUCCESS_DISPLAY_MILLIS = 1_000L
    }
}
