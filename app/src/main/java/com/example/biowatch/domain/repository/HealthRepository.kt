package com.example.biowatch.domain.repository

import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionState
import com.example.biowatch.domain.model.ContinuousAnalysisState
import kotlinx.coroutines.flow.StateFlow

interface HealthRepository {
    val connectionState: StateFlow<HealthServiceConnectionState>
    val heartRate: StateFlow<Int?>
    val collectionState: StateFlow<CollectionState>
    val continuousAnalysisState: StateFlow<ContinuousAnalysisState>

    fun connect()

    fun disconnect()

    fun startContinuousAnalysis()

    fun stopContinuousAnalysis()

    fun startCollection(config: CollectionConfig)

    fun stopCollection()

    fun shareSavedFiles()

    fun deleteSavedFiles(): Boolean
}
