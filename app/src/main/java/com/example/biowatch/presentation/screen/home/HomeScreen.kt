package com.example.biowatch.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.biowatch.R
import com.example.biowatch.presentation.theme.BioWatchTheme
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onSubjectIdChange: (String) -> Unit = {},
    onCollectionLabelChange: (CollectionLabel) -> Unit = {},
    onCollectionPurposeChange: (CollectionPurpose) -> Unit = {},
    onStartCollection: () -> Unit = {},
    onStopCollection: () -> Unit = {},
    onShareFiles: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!uiState.isWatchWorn) {
        WatchNotWornContent(
            onStopTracking = onStopTracking,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(
                R.string.heart_rate_value,
                uiState.heartRate?.toString() ?: "--"
            ),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.heart_rate_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        DashboardDetails(uiState = uiState)

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = if (uiState.isTracking) onStopTracking else onStartTracking
        ) {
            Text(
                text = stringResource(
                    if (uiState.isTracking) {
                        R.string.stop_measurement
                    } else {
                        R.string.start_measurement
                    }
                )
            )
        }

        if (uiState.isTracking) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.collection_title),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = uiState.subjectId,
                onValueChange = onSubjectIdChange,
                enabled = !uiState.isCollecting,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChoiceRow(
                firstLabel = stringResource(R.string.collection_state_normal),
                secondLabel = stringResource(R.string.collection_state_unknown),
                firstSelected = uiState.collectionLabel == CollectionLabel.NORMAL,
                enabled = !uiState.isCollecting,
                onFirst = { onCollectionLabelChange(CollectionLabel.NORMAL) },
                onSecond = { onCollectionLabelChange(CollectionLabel.UNKNOWN) }
            )
            ChoiceRow(
                firstLabel = stringResource(R.string.collection_purpose_calibration),
                secondLabel = stringResource(R.string.collection_purpose_evaluation),
                firstSelected = uiState.collectionPurpose == CollectionPurpose.CALIBRATION,
                enabled = !uiState.isCollecting,
                onFirst = { onCollectionPurposeChange(CollectionPurpose.CALIBRATION) },
                onSecond = { onCollectionPurposeChange(CollectionPurpose.EVALUATION) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.collection_stats,
                    formatElapsedTime(uiState.collectionElapsedSeconds),
                    uiState.sampleCount,
                    uiState.samplingRateHz
                ),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            uiState.collectionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = if (uiState.isCollecting) onStopCollection else onStartCollection,
                enabled = uiState.isCollecting || uiState.subjectId.isNotBlank()
            ) {
                Text(
                    stringResource(
                        if (uiState.isCollecting) {
                            R.string.stop_and_save_collection
                        } else {
                            R.string.start_collection
                        }
                    )
                )
            }
            if (uiState.canShare && !uiState.isCollecting) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onShareFiles) {
                    Text(stringResource(R.string.share_saved_files))
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    firstLabel: String,
    secondLabel: String,
    firstSelected: Boolean,
    enabled: Boolean,
    onFirst: () -> Unit,
    onSecond: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onFirst,
            enabled = enabled && !firstSelected
        ) {
            Text(firstLabel, style = MaterialTheme.typography.labelSmall)
        }
        Button(
            onClick = onSecond,
            enabled = enabled && firstSelected
        ) {
            Text(secondLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatElapsedTime(totalSeconds: Long): String = String.format(
    Locale.US,
    "%02d:%02d",
    totalSeconds / 60,
    totalSeconds % 60
)

@Composable
private fun WatchNotWornContent(
    onStopTracking: () -> Unit,
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
private fun DashboardDetails(uiState: HomeUiState, modifier: Modifier = Modifier) {
    DashboardValue(
        label = stringResource(R.string.status_label),
        value = stringResource(uiState.status.stringResource),
        modifier = modifier
    )
}

private val HomeStatus.stringResource: Int
    @StringRes get() = when (this) {
        HomeStatus.DISCONNECTED -> R.string.status_disconnected
        HomeStatus.CONNECTING -> R.string.status_connecting
        HomeStatus.PREPARING -> R.string.status_preparing
        HomeStatus.MEASURING -> R.string.status_measuring
        HomeStatus.WATCH_NOT_WORN -> R.string.status_watch_not_worn
        HomeStatus.NOT_SUPPORTED -> R.string.status_not_supported
        HomeStatus.PERMISSION_REQUIRED -> R.string.status_permission_required
        HomeStatus.ADJUST_WATCH -> R.string.status_adjust_watch
        HomeStatus.ERROR -> R.string.status_error
    }

@Composable
private fun DashboardValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    BioWatchTheme {
        HomeScreen(uiState = HomeUiState())
    }
}
