package com.example.biowatch.domain.repository

import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionState
import kotlinx.coroutines.flow.StateFlow

interface HealthRepository {
    val connectionState: StateFlow<HealthServiceConnectionState>
    val heartRate: StateFlow<Int?>
    val collectionState: StateFlow<CollectionState>

    fun connect()

    fun disconnect()

    fun startCollection(config: CollectionConfig)

    fun stopCollection()

    fun shareSavedFiles()

    fun deleteSavedFiles(): Boolean
}
