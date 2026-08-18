package com.example.biowatch.data.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import com.example.biowatch.domain.model.AccelerationSample
import com.example.biowatch.domain.model.CollectionConfig
import com.example.biowatch.domain.model.CollectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class SensorDataRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var writer: BufferedWriter? = null
    private var csvFile: File? = null
    private var jsonFile: File? = null
    private var config: CollectionConfig? = null
    private var startTimestampMillis = 0L
    private var lastTimestampMillis = Long.MIN_VALUE
    private val ppgTimestamps = mutableListOf<Long>()
    private val elapsedRealtimeOffsetMillis =
        System.currentTimeMillis() - SystemClock.elapsedRealtime()

    @Synchronized
    fun start(collectionConfig: CollectionConfig): CollectionState {
        check(writer == null) { "이미 데이터 수집이 진행 중입니다." }

        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            DIRECTORY_NAME
        ).apply { mkdirs() }
        check(directory.isDirectory) { "데이터 저장 폴더를 만들 수 없습니다." }

        startTimestampMillis = System.currentTimeMillis()
        val baseName = buildBaseName(collectionConfig, startTimestampMillis)
        csvFile = File(directory, "$baseName.csv")
        jsonFile = File(directory, "$baseName.json")
        config = collectionConfig
        lastTimestampMillis = Long.MIN_VALUE
        ppgTimestamps.clear()

        writer = BufferedWriter(
            OutputStreamWriter(csvFile!!.outputStream(), Charsets.UTF_8)
        ).also {
            it.write(CSV_HEADER)
            it.newLine()
            it.flush()
        }

        return CollectionState(
            isCollecting = true,
            startTimestampMillis = startTimestampMillis
        )
    }

    @Synchronized
    fun recordPpg(
        sensorTimestamp: Long,
        heartRate: Int?,
        ppgGreen: Int,
        ppgStatus: Int,
        isOffBody: Int,
        acceleration: AccelerationSample?,
        ppgRed: Int? = null,
        ppgIr: Int? = null,
        redStatus: Int? = null,
        irStatus: Int? = null
    ): CollectionState? {
        val activeWriter = writer ?: return null
        val timestampMillis = toEpochMillis(sensorTimestamp)
        if (timestampMillis <= lastTimestampMillis) return null

        lastTimestampMillis = timestampMillis
        ppgTimestamps += timestampMillis
        activeWriter.write(formatTimestamp(timestampMillis))
        activeWriter.write(",")
        activeWriter.write(heartRate?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(ppgGreen.toString())
        activeWriter.write(",")
        activeWriter.write(ppgStatus.toString())
        activeWriter.write(",")
        activeWriter.write(isOffBody.toString())
        activeWriter.write(",")
        activeWriter.write(acceleration?.x?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(acceleration?.y?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(acceleration?.z?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(ppgRed?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(ppgIr?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(redStatus?.toString().orEmpty())
        activeWriter.write(",")
        activeWriter.write(irStatus?.toString().orEmpty())
        activeWriter.newLine()
        if (ppgTimestamps.size % FLUSH_INTERVAL == 0) activeWriter.flush()

        return CollectionState(
            isCollecting = true,
            startTimestampMillis = startTimestampMillis,
            sampleCount = ppgTimestamps.size.toLong(),
            samplingRateHz = calculateSamplingRate()
        )
    }

    @Synchronized
    fun stop(ppgSupported: Boolean, accelerometerSupported: Boolean): CollectionState {
        val activeConfig = config
        val activeCsvFile = csvFile
        val activeJsonFile = jsonFile
        val endTimestampMillis = System.currentTimeMillis()
        val samplingRate = calculateSamplingRate()
        val droppedSampleCount = calculateDroppedSamples()

        writer?.flush()
        writer?.close()
        writer = null

        if (activeConfig != null && activeCsvFile != null && activeJsonFile != null) {
            writeMetadata(
                file = activeJsonFile,
                collectionConfig = activeConfig,
                endTimestampMillis = endTimestampMillis,
                samplingRate = samplingRate,
                droppedSampleCount = droppedSampleCount,
                ppgSupported = ppgSupported,
                accelerometerSupported = accelerometerSupported
            )
        }

        val result = CollectionState(
            isCollecting = false,
            startTimestampMillis = startTimestampMillis.takeIf { it > 0 },
            sampleCount = ppgTimestamps.size.toLong(),
            samplingRateHz = samplingRate,
            ppgSupported = ppgSupported,
            accelerometerSupported = accelerometerSupported,
            savedCsvPath = activeCsvFile?.absolutePath,
            savedJsonPath = activeJsonFile?.absolutePath
        )
        config = null
        csvFile = null
        jsonFile = null
        return result
    }

    private fun writeMetadata(
        file: File,
        collectionConfig: CollectionConfig,
        endTimestampMillis: Long,
        samplingRate: Double,
        droppedSampleCount: Long,
        ppgSupported: Boolean,
        accelerometerSupported: Boolean
    ) {
        val metadata = JSONObject()
            .put("subject_id", collectionConfig.subjectId)
            .put("state", collectionConfig.state.value)
            .put("purpose", collectionConfig.purpose.value)
            .put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put(
                "app_version",
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    ?: "unknown"
            )
            .put("start_timestamp", formatTimestamp(startTimestampMillis))
            .put("end_timestamp", formatTimestamp(endTimestampMillis))
            .put("ppg_sampling_rate_hz", samplingRate)
            .put("ppg_unit", "Samsung Health Sensor SDK 원본 정수값(공개 단위 없음)")
            .put("accelerometer_unit", "Samsung Health Sensor SDK 원본 정수값(공개 단위 없음)")
            .put("ppg_stat_meaning", "Samsung Health Sensor SDK PPG_GREEN STATUS 원본 코드")
            .put("is_offbody_meaning", "0=착용 또는 확인 불가, 1=미착용")
            .put("dropped_sample_count", droppedSampleCount)
            .put("ppg_supported", ppgSupported)
            .put("accelerometer_supported", accelerometerSupported)
        file.writeText(metadata.toString(2), Charsets.UTF_8)
    }

    private fun calculateSamplingRate(): Double {
        if (ppgTimestamps.size < 2) return 0.0
        val durationSeconds = (ppgTimestamps.last() - ppgTimestamps.first()) / 1_000.0
        return if (durationSeconds > 0) (ppgTimestamps.size - 1) / durationSeconds else 0.0
    }

    private fun calculateDroppedSamples(): Long {
        if (ppgTimestamps.size < 3) return 0
        val intervals = ppgTimestamps.zipWithNext { first, second -> second - first }
            .filter { it > 0 }
            .sorted()
        if (intervals.isEmpty()) return 0
        val medianInterval = intervals[intervals.size / 2].toDouble()
        return intervals.sumOf { interval ->
            ((interval / medianInterval).roundToLong() - 1L).coerceAtLeast(0L)
        }
    }

    private fun toEpochMillis(sensorTimestamp: Long): Long = when {
        sensorTimestamp in MIN_EPOCH_MILLIS..MAX_EPOCH_MILLIS -> sensorTimestamp
        sensorTimestamp > NANOSECOND_THRESHOLD ->
            elapsedRealtimeOffsetMillis + sensorTimestamp / 1_000_000L
        else -> elapsedRealtimeOffsetMillis + sensorTimestamp
    }

    private fun buildBaseName(config: CollectionConfig, timestampMillis: Long): String {
        val timestamp = FILE_TIMESTAMP_FORMATTER.format(
            Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        )
        return "SamsungWatch_${config.subjectId}_${config.state.value}_${config.purpose.value}_$timestamp"
    }

    private fun formatTimestamp(timestampMillis: Long): String =
        CSV_TIMESTAMP_FORMATTER.format(
            Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        )

    private companion object {
        const val DIRECTORY_NAME = "BioWatch"
        const val CSV_HEADER =
            "Timestamp,HR,PPG_GREEN,PPG_STAT,IS_OFFBODY,ACC_X,ACC_Y,ACC_Z," +
                "PPG_RED,PPG_IR,RED_STAT,IR_STAT"
        const val FLUSH_INTERVAL = 25
        const val MIN_EPOCH_MILLIS = 946_684_800_000L
        const val MAX_EPOCH_MILLIS = 4_102_444_800_000L
        const val NANOSECOND_THRESHOLD = 100_000_000_000_000L
        val FILE_TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
        val CSV_TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    }
}
