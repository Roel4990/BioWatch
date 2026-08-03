package com.example.biowatch.data.repository

import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.domain.repository.HealthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val healthDataSource: HealthDataSource
) : HealthRepository {

    override suspend fun getHeartRate(): Double {
        TODO("Will be implemented in Issue #3")
    }
}
