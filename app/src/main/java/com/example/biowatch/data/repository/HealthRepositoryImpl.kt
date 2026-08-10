package com.example.biowatch.data.repository

import android.content.Context
import android.util.Log
import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.data.service.HeartRateForegroundService
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthDataSource: HealthDataSource,
    @ApplicationContext private val context: Context
) : HealthRepository {

    override val connectionState: StateFlow<HealthServiceConnectionState> =
        healthDataSource.connectionState

    override val heartRate: StateFlow<Int?> = healthDataSource.heartRate

    override fun connect() {
        runCatching { HeartRateForegroundService.start(context) }
            .onFailure { Log.e(TAG, "Failed to start heart rate service", it) }
    }

    override fun disconnect() = HeartRateForegroundService.stop(context)

    private companion object {
        const val TAG = "HealthRepository"
    }
}
