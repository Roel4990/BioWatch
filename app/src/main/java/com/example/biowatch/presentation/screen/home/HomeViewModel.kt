package com.example.biowatch.presentation.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val permissionDenied = MutableStateFlow(false)

    val uiState = combine(
        healthRepository.connectionState,
        healthRepository.heartRate,
        permissionDenied
    ) { connectionState, heartRate, isPermissionDenied ->
        HomeUiState(
            heartRate = heartRate,
            isWatchWorn = connectionState != HealthServiceConnectionState.WatchNotWorn,
            isTracking = !isPermissionDenied &&
                connectionState != HealthServiceConnectionState.Disconnected,
            status = if (isPermissionDenied) {
                HomeStatus.PERMISSION_REQUIRED
            } else {
                connectionState.toHomeStatus()
            }
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
    }
}
