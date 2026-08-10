package com.example.biowatch.domain.model

sealed interface HealthServiceConnectionState {
    data object Disconnected : HealthServiceConnectionState
    data object Connecting : HealthServiceConnectionState
    data object WaitingForHeartRate : HealthServiceConnectionState
    data object Connected : HealthServiceConnectionState
    data object WatchNotWorn : HealthServiceConnectionState
    data object HeartRateUnsupported : HealthServiceConnectionState

    data class MeasurementUnavailable(
        val message: String
    ) : HealthServiceConnectionState

    data class Error(
        val code: Int?,
        val message: String,
        val hasResolution: Boolean
    ) : HealthServiceConnectionState
}
