package com.example.biowatch.data.storage

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
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
        preferences.toSavedBaseline(preferences.activeSubjectId())
    }

    fun baselineForSubject(subjectId: String): Flow<SavedBaseline> =
        context.baselineDataStore.data.map { preferences ->
            preferences.toSavedBaseline(subjectId)
        }

    suspend fun save(
        subjectId: String,
        baselineId: String,
        createdAt: String,
        averageHeartRate: Double? = null
    ) {
        require(subjectId.isNotBlank()) { "사용자 ID를 입력해 주세요." }
        context.baselineDataStore.edit { preferences ->
            preferences.migrateLegacyBaseline()
            preferences[ACTIVE_SUBJECT_ID] = subjectId
            preferences[baselineIdKey(subjectId)] = baselineId
            preferences[baselineCreatedAtKey(subjectId)] = createdAt
            if (averageHeartRate == null) {
                preferences.remove(averageHeartRateKey(subjectId))
            } else {
                preferences[averageHeartRateKey(subjectId)] = averageHeartRate
            }
        }
    }

    suspend fun saveSubjectId(subjectId: String) {
        if (subjectId.isBlank()) return
        context.baselineDataStore.edit { preferences ->
            preferences.migrateLegacyBaseline()
            preferences[ACTIVE_SUBJECT_ID] = subjectId
        }
    }

    suspend fun clearBaseline() {
        context.baselineDataStore.edit { preferences ->
            preferences.migrateLegacyBaseline()
            val subjectId = preferences.activeSubjectId()
            preferences.remove(baselineIdKey(subjectId))
            preferences.remove(baselineCreatedAtKey(subjectId))
            preferences.remove(averageHeartRateKey(subjectId))
        }
    }

    private fun Preferences.activeSubjectId(): String =
        this[ACTIVE_SUBJECT_ID] ?: DEFAULT_SUBJECT_ID

    private fun Preferences.toSavedBaseline(subjectId: String): SavedBaseline {
        val storedBaselineId = this[baselineIdKey(subjectId)]
        val canUseLegacyBaseline =
            storedBaselineId == null &&
                activeSubjectId() == subjectId &&
                this[LEGACY_BASELINE_ID] != null
        return SavedBaseline(
            subjectId = subjectId,
            baselineId = storedBaselineId
                ?: this[LEGACY_BASELINE_ID].takeIf { canUseLegacyBaseline },
            baselineCreatedAt = if (canUseLegacyBaseline) {
                this[LEGACY_BASELINE_CREATED_AT]
            } else {
                this[baselineCreatedAtKey(subjectId)]
            },
            averageHeartRate = if (canUseLegacyBaseline) {
                this[LEGACY_AVERAGE_HEART_RATE]
            } else {
                this[averageHeartRateKey(subjectId)]
            }
        )
    }

    private fun MutablePreferences.migrateLegacyBaseline() {
        val legacyBaselineId = this[LEGACY_BASELINE_ID] ?: return
        val legacySubjectId = activeSubjectId()
        val targetBaselineKey = baselineIdKey(legacySubjectId)
        if (this[targetBaselineKey] == null) {
            this[targetBaselineKey] = legacyBaselineId
            this[LEGACY_BASELINE_CREATED_AT]?.let {
                this[baselineCreatedAtKey(legacySubjectId)] = it
            }
            this[LEGACY_AVERAGE_HEART_RATE]?.let {
                this[averageHeartRateKey(legacySubjectId)] = it
            }
        }
        remove(LEGACY_BASELINE_ID)
        remove(LEGACY_BASELINE_CREATED_AT)
        remove(LEGACY_AVERAGE_HEART_RATE)
    }

    private companion object {
        const val DEFAULT_SUBJECT_ID = "subject_01"
        const val BASELINE_ID_PREFIX = "baseline_id_"
        const val BASELINE_CREATED_AT_PREFIX = "baseline_created_at_"
        const val AVERAGE_HEART_RATE_PREFIX = "average_heart_rate_"

        val ACTIVE_SUBJECT_ID = stringPreferencesKey("subject_id")
        val LEGACY_BASELINE_ID = stringPreferencesKey("baseline_id")
        val LEGACY_BASELINE_CREATED_AT = stringPreferencesKey("baseline_created_at")
        val LEGACY_AVERAGE_HEART_RATE = doublePreferencesKey("average_heart_rate")

        fun baselineIdKey(subjectId: String) =
            stringPreferencesKey(BASELINE_ID_PREFIX + subjectId)

        fun baselineCreatedAtKey(subjectId: String) =
            stringPreferencesKey(BASELINE_CREATED_AT_PREFIX + subjectId)

        fun averageHeartRateKey(subjectId: String) =
            doublePreferencesKey(AVERAGE_HEART_RATE_PREFIX + subjectId)
    }
}
