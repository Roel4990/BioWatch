package com.example.biowatch.domain.repository

interface HealthRepository {
    suspend fun getHeartRate(): Double
}
