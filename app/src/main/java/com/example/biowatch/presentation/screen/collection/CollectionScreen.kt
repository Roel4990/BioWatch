package com.example.biowatch.presentation.screen.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.biowatch.R
import com.example.biowatch.domain.model.CollectionLabel
import com.example.biowatch.domain.model.CollectionPurpose
import com.example.biowatch.presentation.screen.home.HomeUiState
import com.example.biowatch.presentation.theme.BioWatchTheme
import java.util.Locale

@Composable
fun CollectionScreen(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onSubjectIdChange: (String) -> Unit,
    onCollectionLabelChange: (CollectionLabel) -> Unit,
    onCollectionPurposeChange: (CollectionPurpose) -> Unit,
    onStartCollection: () -> Unit,
    onStopCollection: () -> Unit,
    onShareFiles: () -> Unit,
    onCheckServer: () -> Unit,
    onUploadSavedData: () -> Unit,
    onUploadStressPrediction: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = stringResource(R.string.collection_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                if (uiState.hasBaseline) {
                    R.string.baseline_ready
                } else {
                    R.string.baseline_required
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.hasBaseline) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            textAlign = TextAlign.Center
        )
        if (uiState.hasBaseline) {
            Text(
                text = stringResource(R.string.baseline_choice_guide),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!uiState.isTracking) {
            Text(
                text = stringResource(R.string.collection_tracking_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) {
                Text(stringResource(R.string.back_to_home))
            }
            return@Column
        }

        if (!uiState.isWatchWorn) {
            Text(
                text = stringResource(R.string.watch_not_worn_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

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
        Text(
            text = stringResource(
                R.string.collection_current_hr,
                uiState.heartRate?.toString() ?: "--"
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
            enabled = uiState.isCollecting ||
                (uiState.subjectId.isNotBlank() && uiState.isWatchWorn)
        ) {
            Text(
                stringResource(
                    if (uiState.isCollecting) {
                        R.string.stop_and_save_collection
                    } else {
                        R.string.start_collection
                    }
                ),
                textAlign = TextAlign.Center
            )
        }
        if (uiState.canShare && !uiState.isCollecting) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onShareFiles) {
                Text(stringResource(R.string.share_saved_files))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUploadSavedData,
                enabled = !uiState.isAnalysisLoading
            ) {
                Text(
                    stringResource(
                        if (uiState.collectionPurpose == CollectionPurpose.CALIBRATION) {
                            R.string.create_personal_baseline
                        } else {
                            R.string.request_prediction
                        }
                    ),
                    textAlign = TextAlign.Center
                )
            }
            if (uiState.collectionPurpose == CollectionPurpose.EVALUATION) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUploadStressPrediction,
                    enabled = !uiState.isAnalysisLoading
                ) {
                    Text(
                        stringResource(R.string.request_acute_stress_prediction),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCheckServer, enabled = !uiState.isAnalysisLoading) {
            Text(stringResource(R.string.check_analysis_server), textAlign = TextAlign.Center)
        }
        uiState.analysisMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text(stringResource(R.string.back_to_home))
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
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChoiceButton(
            label = firstLabel,
            selected = firstSelected,
            enabled = enabled,
            onClick = onFirst
        )
        ChoiceButton(
            label = secondLabel,
            selected = !firstSelected,
            enabled = enabled,
            onClick = onSecond
        )
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .width(104.dp)
            .height(40.dp)
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled && !selected, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun formatElapsedTime(totalSeconds: Long): String = String.format(
    Locale.US,
    "%02d:%02d",
    totalSeconds / 60,
    totalSeconds % 60
)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun CollectionScreenPreview() {
    BioWatchTheme {
        CollectionScreen(
            uiState = HomeUiState(isTracking = true),
            onBack = {},
            onSubjectIdChange = {},
            onCollectionLabelChange = {},
            onCollectionPurposeChange = {},
            onStartCollection = {},
            onStopCollection = {},
            onShareFiles = {},
            onCheckServer = {},
            onUploadSavedData = {},
            onUploadStressPrediction = {}
        )
    }
}
