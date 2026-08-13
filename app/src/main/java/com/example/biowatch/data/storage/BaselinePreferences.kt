package com.example.biowatch.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.baselineDataStore by preferencesDataStore(name = "analysis_baseline")

data class SavedBaseline(
    val subjectId: String = "subject_01",
    val baselineId: String? = null,
    val baselineCreatedAt: String? = null,
    val averageHeartRate: Double? = null
)

@Singleton
class BaselinePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val savedBaseline: Flow<SavedBaseline> = context.baselineDataStore.data.map { preferences ->
        SavedBaseline(
            subjectId = preferences[SUBJECT_ID] ?: "subject_01",
            baselineId = preferences[BASELINE_ID],
            baselineCreatedAt = preferences[BASELINE_CREATED_AT],
            averageHeartRate = preferences[AVERAGE_HEART_RATE]
        )
    }

    suspend fun save(
        subjectId: String,
        baselineId: String,
        createdAt: String,
        averageHeartRate: Double? = null
    ) {
        context.baselineDataStore.edit { preferences ->
            preferences[SUBJECT_ID] = subjectId
            preferences[BASELINE_ID] = baselineId
            preferences[BASELINE_CREATED_AT] = createdAt
            if (averageHeartRate == null) {
                preferences.remove(AVERAGE_HEART_RATE)
            } else {
                preferences[AVERAGE_HEART_RATE] = averageHeartRate
            }
        }
    }

    suspend fun saveSubjectId(subjectId: String) {
        context.baselineDataStore.edit { preferences ->
            if (preferences[SUBJECT_ID] != subjectId) {
                preferences.remove(BASELINE_ID)
                preferences.remove(BASELINE_CREATED_AT)
                preferences.remove(AVERAGE_HEART_RATE)
            }
            preferences[SUBJECT_ID] = subjectId
        }
    }

    suspend fun clearBaseline() {
        context.baselineDataStore.edit { preferences ->
            preferences.remove(BASELINE_ID)
            preferences.remove(BASELINE_CREATED_AT)
            preferences.remove(AVERAGE_HEART_RATE)
        }
    }

    private companion object {
        val SUBJECT_ID = stringPreferencesKey("subject_id")
        val BASELINE_ID = stringPreferencesKey("baseline_id")
        val BASELINE_CREATED_AT = stringPreferencesKey("baseline_created_at")
        val AVERAGE_HEART_RATE = doublePreferencesKey("average_heart_rate")
    }
}
