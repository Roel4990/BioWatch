package com.example.biowatch.data.network

import com.example.biowatch.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ServerHealth(
    val status: String,
    val modelLoaded: Boolean,
    val modelVersion: String,
    val deploymentStage: String,
    val fallModelLoaded: Boolean,
    val fallModelVersion: String
)

data class CalibrationResult(
    val baselineId: String,
    val validWindowCount: Int,
    val status: String,
    val createdAt: String
)

data class PredictionResult(
    val result: String,
    val abnormalProbability: Double?,
    val validWindowCount: Int,
    val modelVersion: String,
    val reason: String?
)

data class StressPredictionResult(
    val result: String,
    val acuteStressProbability: Double?,
    val validWindowCount: Int,
    val rejectedWindowCount: Int,
    val modelVersion: String,
    val reason: String?
)

data class FallInputQuality(
    val detectedAccelUnit: String,
    val observedSamplingRateHz: Double?,
    val rawSampleCount: Int,
    val resampledSampleCount: Int,
    val durationSec: Double?
)

data class FallPredictionResult(
    val analysisType: String,
    val subjectId: String?,
    val result: String,
    val fallProbability: Double?,
    val threshold: Double?,
    val eventTimeSec: Double?,
    val validWindowCount: Int,
    val fallCandidateWindowCount: Int,
    val maxConsecutiveCandidateWindows: Int,
    val minimumConsecutiveWindows: Int,
    val inputQuality: FallInputQuality?,
    val modelVersion: String,
    val deploymentStage: String,
    val reason: String?,
    val medicalDiagnosis: Boolean
)

@Singleton
class AnalysisApiClient @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun health(): ServerHealth = execute(
        Request.Builder().url(endpoint("/health")).get().build()
    ) { json ->
        ServerHealth(
            status = json.getString("status"),
            modelLoaded = json.getBoolean("model_loaded"),
            modelVersion = json.optString("model_version"),
            deploymentStage = json.optString("deployment_stage"),
            fallModelLoaded = json.optBoolean("fall_model_loaded", false),
            fallModelVersion = json.optString("fall_model_version")
        )
    }

    suspend fun calibrate(subjectId: String, csvFile: File): CalibrationResult {
        val json = upload(
            path = "/api/v1/calibrations",
            fields = mapOf("subject_id" to subjectId),
            csvFile = csvFile
        )
        return CalibrationResult(
            baselineId = json.getString("baseline_id"),
            validWindowCount = json.optInt("valid_window_count"),
            status = json.optString("status"),
            createdAt = json.getString("created_at")
        )
    }

    suspend fun predict(
        subjectId: String,
        baselineId: String,
        csvFile: File
    ): PredictionResult {
        val json = upload(
            path = "/api/v1/predictions",
            fields = mapOf(
                "subject_id" to subjectId,
                "baseline_id" to baselineId
            ),
            csvFile = csvFile
        )
        return PredictionResult(
            result = json.getString("result"),
            abnormalProbability = if (json.isNull("abnormal_probability")) null
                else json.optDouble("abnormal_probability"),
            validWindowCount = json.optInt("valid_window_count"),
            modelVersion = json.optString("model_version"),
            reason = json.optString("reason").takeIf { it.isNotBlank() }
        )
    }

    suspend fun predictStress(
        subjectId: String,
        baselineId: String,
        csvFile: File
    ): StressPredictionResult {
        val json = upload(
            path = "/api/v1/stress/predictions",
            fields = mapOf(
                "subject_id" to subjectId,
                "baseline_id" to baselineId
            ),
            csvFile = csvFile
        )
        return StressPredictionResult(
            result = json.getString("result"),
            acuteStressProbability = if (json.isNull("acute_stress_probability")) null
                else json.optDouble("acute_stress_probability"),
            validWindowCount = json.optInt("valid_window_count"),
            rejectedWindowCount = json.optInt("rejected_window_count"),
            modelVersion = json.optString("model_version"),
            reason = json.optString("reason").takeIf { it.isNotBlank() }
        )
    }

    suspend fun predictFall(
        subjectId: String,
        csvFile: File
    ): FallPredictionResult {
        val json = upload(
            path = "/api/v1/fall/predictions",
            fields = mapOf("subject_id" to subjectId),
            csvFile = csvFile
        )
        val inputQuality = json.optJSONObject("input_quality")?.let { quality ->
            FallInputQuality(
                detectedAccelUnit = quality.optString("detected_accel_unit"),
                observedSamplingRateHz = quality.optionalDouble("observed_sampling_rate_hz"),
                rawSampleCount = quality.optInt("raw_sample_count"),
                resampledSampleCount = quality.optInt("resampled_sample_count"),
                durationSec = quality.optionalDouble("duration_sec")
            )
        }
        return FallPredictionResult(
            analysisType = json.optString("analysis_type"),
            subjectId = json.optString("subject_id").takeIf(String::isNotBlank),
            result = json.getString("result"),
            fallProbability = json.optionalDouble("fall_probability"),
            threshold = json.optionalDouble("threshold"),
            eventTimeSec = json.optionalDouble("event_time_sec"),
            validWindowCount = json.optInt("valid_window_count"),
            fallCandidateWindowCount = json.optInt("fall_candidate_window_count"),
            maxConsecutiveCandidateWindows =
                json.optInt("max_consecutive_candidate_windows"),
            minimumConsecutiveWindows = json.optInt("minimum_consecutive_windows"),
            inputQuality = inputQuality,
            modelVersion = json.optString("model_version"),
            deploymentStage = json.optString("deployment_stage"),
            reason = json.optString("reason").takeIf { it.isNotBlank() },
            medicalDiagnosis = json.optBoolean("medical_diagnosis", false)
        )
    }

    private suspend fun upload(
        path: String,
        fields: Map<String, String>,
        csvFile: File
    ): JSONObject = withContext(Dispatchers.IO) {
        require(csvFile.exists()) { "저장된 CSV 파일을 찾을 수 없습니다." }
        require(csvFile.length() <= MAX_UPLOAD_BYTES) { "CSV 파일이 5MB를 초과합니다." }
        require(BuildConfig.ANALYSIS_API_TOKEN.isNotBlank()) { ".env에 API 토큰을 입력해 주세요." }

        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            fields.forEach { (name, value) -> addFormDataPart(name, value) }
            addFormDataPart(
                "file",
                csvFile.name,
                csvFile.asRequestBody(CSV_MEDIA_TYPE)
            )
        }.build()
        val request = Request.Builder()
            .url(endpoint(path))
            .header("Authorization", "Bearer ${BuildConfig.ANALYSIS_API_TOKEN}")
            .post(body)
            .build()
        executeRequest(request)
    }

    private suspend fun <T> execute(request: Request, parse: (JSONObject) -> T): T =
        withContext(Dispatchers.IO) { parse(executeRequest(request)) }

    private fun executeRequest(request: Request): JSONObject =
        httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(responseText).optString("detail") }.getOrNull()
                val message = when (response.code) {
                    401 -> "서버 인증에 실패했습니다. API 토큰을 확인해 주세요."
                    413 -> "CSV 파일이 서버 허용 크기인 5MB를 초과했습니다."
                    415 -> "서버가 전송한 CSV 파일 형식을 지원하지 않습니다."
                    else -> detail?.takeIf { it.isNotBlank() }
                        ?: "서버 요청에 실패했습니다. (${response.code})"
                }
                throw IllegalStateException(message)
            }
            JSONObject(responseText)
        }

    private fun endpoint(path: String): String {
        val baseUrl = BuildConfig.ANALYSIS_API_BASE_URL.trim().trimEnd('/')
        require(baseUrl.isNotBlank()) { ".env에 서버 주소를 입력해 주세요." }
        require(!baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            "워치에서 접근 가능한 서버 PC의 IPv4 주소를 입력해 주세요."
        }
        return baseUrl + path
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 5L * 1024 * 1024
        val CSV_MEDIA_TYPE = "text/csv; charset=utf-8".toMediaType()
    }
}

private fun JSONObject.optionalDouble(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null
