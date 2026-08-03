package com.example.biowatch.data.datasource

interface HealthDataSource {
    suspend fun getHeartRate(): Double
}
