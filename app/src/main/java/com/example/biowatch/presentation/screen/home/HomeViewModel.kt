package com.example.biowatch.presentation.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.domain.repository.HealthRepository
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
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val permissionDenied = MutableStateFlow(false)
    private val subjectId = MutableStateFlow("subject_01")
    private val collectionLabel = MutableStateFlow(CollectionLabel.NORMAL)
    private val collectionPurpose = MutableStateFlow(CollectionPurpose.CALIBRATION)
    private val elapsedSeconds = MutableStateFlow(0L)
    private val collectionForm = combine(
        subjectId,
        collectionLabel,
        collectionPurpose,
        elapsedSeconds
    ) { id, label, purpose, elapsed ->
        CollectionForm(id, label, purpose, elapsed)
    }

    val uiState = combine(
        healthRepository.connectionState,
        healthRepository.heartRate,
        healthRepository.collectionState,
        permissionDenied,
        collectionForm
    ) { connectionState, heartRate, collectionState, isPermissionDenied, form ->
        HomeUiState(
            heartRate = heartRate,
            isWatchWorn = connectionState != HealthServiceConnectionState.WatchNotWorn,
            isTracking = !isPermissionDenied &&
                connectionState != HealthServiceConnectionState.Disconnected,
            status = if (isPermissionDenied) {
                HomeStatus.PERMISSION_REQUIRED
            } else {
                connectionState.toHomeStatus()
            },
            subjectId = form.subjectId,
            collectionLabel = form.label,
            collectionPurpose = form.purpose,
            isCollecting = collectionState.isCollecting,
            collectionElapsedSeconds = form.elapsedSeconds,
            sampleCount = collectionState.sampleCount,
            samplingRateHz = collectionState.samplingRateHz,
            ppgSupported = collectionState.ppgSupported,
            accelerometerSupported = collectionState.accelerometerSupported,
            canShare = collectionState.savedCsvPath != null,
            collectionMessage = collectionState.errorMessage
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
        subjectId.value = value.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(MAX_SUBJECT_ID_LENGTH)
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
    }

    private data class CollectionForm(
        val subjectId: String,
        val label: CollectionLabel,
        val purpose: CollectionPurpose,
        val elapsedSeconds: Long
    )
}
