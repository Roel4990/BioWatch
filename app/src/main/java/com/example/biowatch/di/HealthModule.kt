package com.example.biowatch.di

import com.example.biowatch.data.datasource.HealthDataSource
import com.example.biowatch.data.datasource.HealthServicesManager
import com.example.biowatch.data.repository.HealthRepositoryImpl
import com.example.biowatch.domain.repository.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {

    @Binds
    @Singleton
    abstract fun bindHealthDataSource(
        healthServicesManager: HealthServicesManager
    ): HealthDataSource

    @Binds
    @Singleton
    abstract fun bindHealthRepository(
        healthRepositoryImpl: HealthRepositoryImpl
    ): HealthRepository
}
