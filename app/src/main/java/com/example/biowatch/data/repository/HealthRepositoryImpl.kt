package com.example.biowatch.data.repository

import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.repository.HealthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthDataSource: HealthDataSource
) : HealthRepository {

    override val connectionState: StateFlow<HealthServiceConnectionState> =
        healthDataSource.connectionState

    override val heartRate: StateFlow<Int?> = healthDataSource.heartRate

    override fun connect() = healthDataSource.connect()

    override fun disconnect() = healthDataSource.disconnect()
}
