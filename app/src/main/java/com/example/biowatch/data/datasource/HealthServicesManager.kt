package com.example.biowatch.data.datasource

import android.content.Context
import androidx.health.services.client.HealthServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthServicesManager @Inject constructor(
    @ApplicationContext context: Context
) : HealthDataSource {

    private val measureClient = HealthServices.getClient(context).measureClient

    override suspend fun getHeartRate(): Double {
        TODO("Will be implemented in Issue #3")
    }
}
