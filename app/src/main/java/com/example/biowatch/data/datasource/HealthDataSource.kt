package com.example.biowatch.data.datasource

import com.example.biowatch.domain.model.HealthServiceConnectionState
import kotlinx.coroutines.flow.StateFlow

interface HealthDataSource {
    val connectionState: StateFlow<HealthServiceConnectionState>
    val heartRate: StateFlow<Int?>

    fun connect()

    fun disconnect()
}
