package com.example.biowatch.presentation.screen.home

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.biowatch.R
import com.example.biowatch.domain.model.ContinuousAnalysisPhase
import com.example.biowatch.domain.model.RhythmAnalysisResult
import com.example.biowatch.domain.model.StressAnalysisResult
import com.example.biowatch.presentation.theme.BioWatchTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onChangeSubject: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var titleTapCount by remember { mutableIntStateOf(0) }
    var lastTitleTapMillis by remember { mutableLongStateOf(0L) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var showChangeSubjectDialog by remember { mutableStateOf(false) }
    val requestSubjectChange = { showChangeSubjectDialog = true }
    val onTitleClick = {
        val now = SystemClock.elapsedRealtime()
        titleTapCount = if (now - lastTitleTapMillis <= TITLE_TAP_TIMEOUT_MILLIS) {
            titleTapCount + 1
        } else {
            1
        }
        lastTitleTapMillis = now
        if (titleTapCount >= REQUIRED_TITLE_TAPS) {
            titleTapCount = 0
            showAdminPinDialog = true
        }
    }

    BackHandler(enabled = !showAdminPinDialog && !showChangeSubjectDialog) {
        requestSubjectChange()
    }

    when {
        !uiState.isWatchWorn -> WatchNotWornContent(
            onStopTracking = onStopTracking,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
        !uiState.isTracking -> TrackingStoppedContent(
            onStartTracking = onStartTracking,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
        else -> MonitoringContent(
            uiState = uiState,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
    }

    if (showAdminPinDialog) {
        AdminPinDialog(
            onDismiss = { showAdminPinDialog = false },
            onAuthenticated = {
                showAdminPinDialog = false
                onOpenCollection()
            }
        )
    }

    if (showChangeSubjectDialog) {
        ChangeSubjectDialog(
            onDismiss = { showChangeSubjectDialog = false },
            onConfirm = {
                showChangeSubjectDialog = false
                onChangeSubject()
            }
        )
    }
}

@Composable
private fun MonitoringContent(
    uiState: HomeUiState,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HomeHeader(onTitleClick = onTitleClick)
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartRateMetric(
                value = uiState.heartRate?.toString() ?: "--",
                label = stringResource(R.string.current_heart_rate),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            HeartRateMetric(
                value = uiState.baselineAverageHeartRate?.roundToInt()?.toString() ?: "--",
                label = stringResource(R.string.baseline_average_heart_rate),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        AnalysisProgress(progress = uiState.continuousAnalysisProgress)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = monitoringStatusText(uiState),
            style = MaterialTheme.typography.labelSmall,
            color = monitoringStatusColor(uiState.continuousAnalysisPhase),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnalysisResultCard(
                label = stringResource(R.string.rhythm_analysis_label),
                result = stringResource(uiState.rhythmResult.stringResource),
                probability = uiState.abnormalProbability?.let {
                    stringResource(R.string.abnormal_probability, probabilityPercent(it))
                } ?: stringResource(R.string.probability_waiting),
                emphasized = uiState.rhythmResult == RhythmAnalysisResult.POSSIBLE_ABNORMAL,
                normal = uiState.rhythmResult == RhythmAnalysisResult.NORMAL,
                modifier = Modifier.weight(1f)
            )
            AnalysisResultCard(
                label = stringResource(R.string.stress_analysis_label),
                result = stringResource(uiState.stressResult.stringResource),
                probability = uiState.acuteStressProbability?.let {
                    stringResource(R.string.stress_probability, probabilityPercent(it))
                } ?: stringResource(R.string.probability_waiting),
                emphasized =
                    uiState.stressResult == StressAnalysisResult.POSSIBLE_ACUTE_STRESS,
                normal = uiState.stressResult == StressAnalysisResult.NORMAL,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeartRateMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.compact_heart_rate_value, value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AnalysisProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(3.dp)
            )
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
private fun AnalysisResultCard(
    label: String,
    result: String,
    probability: String,
    emphasized: Boolean,
    normal: Boolean,
    modifier: Modifier = Modifier
) {
    val resultColor = when {
        emphasized -> MaterialTheme.colorScheme.error
        normal -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = resultColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = probability,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun monitoringStatusText(uiState: HomeUiState): String =
    when (uiState.continuousAnalysisPhase) {
        ContinuousAnalysisPhase.STOPPED ->
            stringResource(R.string.monitoring_stopped)
        ContinuousAnalysisPhase.WAITING_FOR_SENSOR ->
            uiState.monitoringMessage ?: stringResource(R.string.monitoring_sensor_waiting)
        ContinuousAnalysisPhase.COLLECTING -> stringResource(
            R.string.monitoring_collecting,
            formatTime(uiState.continuousAnalysisElapsedSeconds),
            formatTime(uiState.continuousAnalysisTargetSeconds)
        )
        ContinuousAnalysisPhase.ANALYZING ->
            stringResource(R.string.monitoring_analyzing)
        ContinuousAnalysisPhase.RESULT ->
            stringResource(R.string.monitoring_result_ready)
        ContinuousAnalysisPhase.RETRYING ->
            uiState.monitoringMessage ?: stringResource(R.string.monitoring_retrying)
        ContinuousAnalysisPhase.BASELINE_REQUIRED ->
            uiState.monitoringMessage ?: stringResource(R.string.monitoring_baseline_required)
    }

@Composable
private fun monitoringStatusColor(phase: ContinuousAnalysisPhase): Color =
    when (phase) {
        ContinuousAnalysisPhase.BASELINE_REQUIRED,
        ContinuousAnalysisPhase.RETRYING -> MaterialTheme.colorScheme.error
        ContinuousAnalysisPhase.RESULT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun TrackingStoppedContent(
    onStartTracking: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HomeHeader(onTitleClick = onTitleClick)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.monitoring_stopped),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onStartTracking) {
            Text(stringResource(R.string.start_measurement))
        }
    }
}

@Composable
private fun WatchNotWornContent(
    onStopTracking: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HomeHeader(onTitleClick = onTitleClick)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.watch_not_worn_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.watch_not_worn_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onStopTracking) {
            Text(text = stringResource(R.string.stop_measurement))
        }
    }
}

@Composable
private fun HomeHeader(
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(onClick = onTitleClick)
        )
    }

}

@Composable
private fun ChangeSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.change_subject_title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.change_subject_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

private val RhythmAnalysisResult.stringResource: Int
    @StringRes get() = when (this) {
        RhythmAnalysisResult.WAITING -> R.string.analysis_waiting
        RhythmAnalysisResult.NORMAL -> R.string.analysis_normal
        RhythmAnalysisResult.POSSIBLE_ABNORMAL -> R.string.analysis_possible_abnormal
        RhythmAnalysisResult.UNAVAILABLE -> R.string.analysis_unavailable
    }

private val StressAnalysisResult.stringResource: Int
    @StringRes get() = when (this) {
        StressAnalysisResult.WAITING -> R.string.analysis_waiting
        StressAnalysisResult.NORMAL -> R.string.analysis_normal
        StressAnalysisResult.POSSIBLE_ACUTE_STRESS -> R.string.analysis_stress_warning
        StressAnalysisResult.UNAVAILABLE -> R.string.analysis_unavailable
    }

private fun probabilityPercent(probability: Double): Int =
    (probability.coerceIn(0.0, 1.0) * 100).roundToInt()

private fun formatTime(totalSeconds: Long): String = String.format(
    Locale.US,
    "%02d:%02d",
    totalSeconds / 60,
    totalSeconds % 60
)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    BioWatchTheme {
        HomeScreen(
            uiState = HomeUiState(
                heartRate = 78,
                isTracking = true,
                baselineAverageHeartRate = 74.2,
                continuousAnalysisPhase = ContinuousAnalysisPhase.COLLECTING,
                continuousAnalysisProgress = 0.52f,
                continuousAnalysisElapsedSeconds = 34,
                rhythmResult = RhythmAnalysisResult.NORMAL,
                abnormalProbability = 0.18,
                stressResult = StressAnalysisResult.NORMAL,
                acuteStressProbability = 0.25,
                lastAnalyzedAtMillis = System.currentTimeMillis()
            )
        )
    }
}

private const val REQUIRED_TITLE_TAPS = 3
private const val TITLE_TAP_TIMEOUT_MILLIS = 1_500L
