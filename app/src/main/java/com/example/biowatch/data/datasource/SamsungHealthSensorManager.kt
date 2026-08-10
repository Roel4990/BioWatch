package com.example.biowatch.data.datasource

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SamsungHealthSensorManager @Inject constructor(
    @ApplicationContext context: Context
) : HealthDataSource {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val offBodySensor = sensorManager?.getDefaultSensor(
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT
    )

    private val _connectionState = MutableStateFlow<HealthServiceConnectionState>(
        HealthServiceConnectionState.Disconnected
    )
    override val connectionState: StateFlow<HealthServiceConnectionState> =
        _connectionState.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    override val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private var heartRateTracker: HealthTracker? = null
    private var serviceConnected = false
    private var trackingRequested = false
    private var isWatchWorn: Boolean? = null

    private val offBodyListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val isOnBody = event.values.firstOrNull() == 1.0f
            isWatchWorn = isOnBody

            if (isOnBody) {
                if (serviceConnected) {
                    startHeartRateTracking()
                    _connectionState.value = HealthServiceConnectionState.WaitingForHeartRate
                }
            } else {
                stopHeartRateTracking()
                _heartRate.value = null
                _connectionState.value = HealthServiceConnectionState.WatchNotWorn
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    private val heartRateListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(dataPoints: List<DataPoint>) {
            dataPoints.lastOrNull()?.let(::handleHeartRateData)
        }

        override fun onFlushCompleted() = Unit

        override fun onError(error: HealthTracker.TrackerError) {
            Log.e(TAG, "Heart rate tracker error: $error")
            _connectionState.value = HealthServiceConnectionState.Error(
                code = null,
                message = "Heart rate tracking failed: $error",
                hasResolution = false
            )
        }
    }

    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            Log.i(TAG, "Connected to Samsung Health Sensor service")
            if (!trackingRequested) {
                runCatching { healthTrackingService.disconnectService() }
                return
            }
            serviceConnected = true
            _connectionState.value = try {
                val supportedTypes = healthTrackingService
                    .trackingCapability
                    .supportHealthTrackerTypes

                if (HealthTrackerType.HEART_RATE_CONTINUOUS in supportedTypes) {
                    if (isWatchWorn == false) {
                        HealthServiceConnectionState.WatchNotWorn
                    } else {
                        startHeartRateTracking()
                        HealthServiceConnectionState.WaitingForHeartRate
                    }
                } else {
                    HealthServiceConnectionState.HeartRateUnsupported
                }
            } catch (exception: RuntimeException) {
                exception.toConnectionError()
            }
        }

        override fun onConnectionEnded() {
            Log.i(TAG, "Samsung Health Sensor service connection ended")
            serviceConnected = false
            heartRateTracker = null
            _connectionState.value = HealthServiceConnectionState.Disconnected
        }

        override fun onConnectionFailed(exception: HealthTrackerException) {
            Log.e(TAG, "Samsung Health Sensor connection failed", exception)
            _connectionState.value = HealthServiceConnectionState.Error(
                code = exception.errorCode,
                message = exception.message
                    ?: "Failed to connect to Samsung Health Sensor service.",
                hasResolution = exception.hasResolution()
            )
        }
    }

    private val healthTrackingService: HealthTrackingService =
        HealthTrackingService(connectionListener, context)

    override fun connect() {
        if (_connectionState.value == HealthServiceConnectionState.Connecting ||
            _connectionState.value == HealthServiceConnectionState.Connected
        ) {
            return
        }

        trackingRequested = true
        _connectionState.value = HealthServiceConnectionState.Connecting
        try {
            Log.i(TAG, "Connecting to Samsung Health Sensor service")
            registerOffBodyListener()
            healthTrackingService.connectService()
        } catch (exception: RuntimeException) {
            _connectionState.value = exception.toConnectionError()
        }
    }

    override fun disconnect() {
        try {
            Log.i(TAG, "Disconnecting from Samsung Health Sensor service")
            stopHeartRateTracking()
            sensorManager?.unregisterListener(offBodyListener)
            runCatching { healthTrackingService.disconnectService() }
        } finally {
            trackingRequested = false
            serviceConnected = false
            isWatchWorn = null
            _heartRate.value = null
            _connectionState.value = HealthServiceConnectionState.Disconnected
        }
    }

    private fun startHeartRateTracking() {
        if (heartRateTracker != null || isWatchWorn == false) {
            return
        }

        heartRateTracker = healthTrackingService
            .getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS)
            .also { it.setEventListener(heartRateListener) }
    }

    private fun stopHeartRateTracking() {
        runCatching { heartRateTracker?.unsetEventListener() }
        heartRateTracker = null
    }

    private fun registerOffBodyListener() {
        offBodySensor?.let { sensor ->
            sensorManager?.registerListener(
                offBodyListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun handleHeartRateData(dataPoint: DataPoint) {
        val status = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)
        val heartRate = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE)

        when (status) {
            HEART_RATE_STATUS_SUCCESS -> {
                if (heartRate > 0) {
                    _heartRate.value = heartRate
                    _connectionState.value = HealthServiceConnectionState.Connected
                }
            }

            HEART_RATE_STATUS_DETACHED -> {
                isWatchWorn = false
                _heartRate.value = null
                _connectionState.value = HealthServiceConnectionState.WatchNotWorn
                if (offBodySensor != null) {
                    stopHeartRateTracking()
                }
            }

            HEART_RATE_STATUS_MOVEMENT,
            HEART_RATE_STATUS_WEAK_SIGNAL,
            HEART_RATE_STATUS_TOO_WEAK_SIGNAL -> {
                _connectionState.value = HealthServiceConnectionState.MeasurementUnavailable(
                    message = "움직이지 말고 워치를 손목에 밀착해 주세요"
                )
            }
        }
    }

    private fun RuntimeException.toConnectionError() = HealthServiceConnectionState.Error(
        code = null,
        message = message ?: "Samsung Health Sensor service is unavailable.",
        hasResolution = false
    )

    private companion object {
        const val TAG = "SamsungHealthSensor"
        const val HEART_RATE_STATUS_TOO_WEAK_SIGNAL = -10
        const val HEART_RATE_STATUS_WEAK_SIGNAL = -8
        const val HEART_RATE_STATUS_DETACHED = -3
        const val HEART_RATE_STATUS_MOVEMENT = -2
        const val HEART_RATE_STATUS_SUCCESS = 1
    }
}
