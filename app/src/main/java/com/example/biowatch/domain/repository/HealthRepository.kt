package com.example.biowatch.domain.repository

import com.example.biowatch.domain.model.HealthServiceConnectionState
import kotlinx.coroutines.flow.StateFlow

interface HealthRepository {
    val connectionState: StateFlow<HealthServiceConnectionState>
    val heartRate: StateFlow<Int?>

    fun connect()

    fun disconnect()
}
