package com.example.biowatch.presentation.screen.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.biowatch.R
import com.example.biowatch.presentation.theme.BioWatchTheme
import java.util.Locale

@Composable
fun CalibrationScreen(
    uiState: CalibrationUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.calibration_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))

        when (uiState.status) {
            CalibrationStatus.PREPARING -> StatusContent(
                message = stringResource(R.string.calibration_preparing)
            )

            CalibrationStatus.MEASURING -> MeasuringContent(uiState)
            CalibrationStatus.UPLOADING -> StatusContent(
                message = stringResource(R.string.calibration_uploading)
            )

            CalibrationStatus.SUCCESS -> StatusContent(
                message = stringResource(R.string.calibration_success)
            )

            CalibrationStatus.ERROR -> {
                Text(
                    text = uiState.message ?: stringResource(R.string.calibration_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry_calibration))
                }
            }
        }
    }
}

@Composable
private fun MeasuringContent(uiState: CalibrationUiState) {
    Text(
        text = stringResource(R.string.calibration_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    CalibrationProgress(
        progress = uiState.progress,
        remainingTime = formatTime(uiState.remainingSeconds)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(
            R.string.calibration_current_hr,
            uiState.currentHeartRate?.toString() ?: "--"
        ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    if (!uiState.isWatchWorn) {
        Text(
            text = stringResource(R.string.calibration_watch_warning),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalibrationProgress(progress: Float, remainingTime: String) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val heartRateComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.heart_rate)
    )

    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 7.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        LottieAnimation(
            composition = heartRateComposition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .offset(y = (-7).dp)
        )
        Text(
            text = remainingTime,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 20.dp)
        )
    }
}

@Composable
private fun StatusContent(message: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

private fun formatTime(totalSeconds: Long): String = String.format(
    Locale.US,
    "%02d:%02d",
    totalSeconds / 60,
    totalSeconds % 60
)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun CalibrationScreenPreview() {
    BioWatchTheme {
        CalibrationScreen(
            uiState = CalibrationUiState(
                status = CalibrationStatus.MEASURING,
                subjectId = "subject_01",
                currentHeartRate = 72,
                elapsedSeconds = 125
            ),
            onRetry = {}
        )
    }
}
