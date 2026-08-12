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
    val modelVersion: String
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
            modelVersion = json.optString("model_version")
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
                throw IllegalStateException(detail?.takeIf { it.isNotBlank() }
                    ?: "서버 요청에 실패했습니다. (${response.code})")
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
