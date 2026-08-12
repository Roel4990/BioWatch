package com.example.biowatch.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.util.Log
import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.data.service.HeartRateForegroundService
import com.example.biowatch.domain.model.HealthServiceConnectionState
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionState
import com.example.biowatch.domain.repository.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthDataSource: HealthDataSource,
    @ApplicationContext private val context: Context
) : HealthRepository {

    override val connectionState: StateFlow<HealthServiceConnectionState> =
        healthDataSource.connectionState

    override val heartRate: StateFlow<Int?> = healthDataSource.heartRate
    override val collectionState: StateFlow<CollectionState> = healthDataSource.collectionState

    override fun connect() {
        runCatching { HeartRateForegroundService.start(context) }
            .onFailure { Log.e(TAG, "Failed to start heart rate service", it) }
    }

    override fun disconnect() = HeartRateForegroundService.stop(context)

    override fun startCollection(config: CollectionConfig) =
        healthDataSource.startCollection(config)

    override fun stopCollection() = healthDataSource.stopCollection()

    override fun shareSavedFiles() {
        val paths = listOfNotNull(
            collectionState.value.savedCsvPath,
            collectionState.value.savedJsonPath
        )
        if (paths.isEmpty()) return

        val uris = ArrayList(paths.map { path ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path)
            )
        })
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun deleteSavedFiles(): Boolean = healthDataSource.deleteSavedFiles()

    private companion object {
        const val TAG = "HealthRepository"
    }
}
