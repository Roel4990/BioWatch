package com.example.biowatch.domain.model

data class CollectionConfig(
    val subjectId: String,
    val state: CollectionLabel,
    val purpose: CollectionPurpose
)

enum class CollectionLabel(val value: String) {
    NORMAL("normal"),
    UNKNOWN("unknown")
}

enum class CollectionPurpose(val value: String) {
    CALIBRATION("calibration"),
    EVALUATION("evaluation")
}

data class CollectionState(
    val isCollecting: Boolean = false,
    val startTimestampMillis: Long? = null,
    val sampleCount: Long = 0,
    val samplingRateHz: Double = 0.0,
    val ppgSupported: Boolean? = null,
    val accelerometerSupported: Boolean? = null,
    val savedCsvPath: String? = null,
    val savedJsonPath: String? = null,
    val errorMessage: String? = null
)

data class AccelerationSample(
    val timestampMillis: Long,
    val x: Int,
    val y: Int,
    val z: Int
)
