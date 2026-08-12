package com.example.biowatch.presentation.screen.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.biowatch.R
import com.example.biowatch.presentation.theme.BioWatchTheme

@Composable
fun SubjectSetupScreen(
    uiState: SubjectSetupUiState,
    onSubjectIdChange: (String) -> Unit,
    onContinue: () -> Unit,
    onUseExistingBaseline: () -> Unit,
    onCreateBaseline: () -> Unit,
    onDismissBaselineChoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.subject_setup_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subject_setup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        BasicTextField(
            value = uiState.subjectId,
            onValueChange = onSubjectIdChange,
            enabled = !uiState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (uiState.hasMatchingBaseline) {
                    R.string.subject_baseline_found
                } else {
                    R.string.subject_baseline_not_found
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (uiState.hasMatchingBaseline) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            enabled = !uiState.isLoading && uiState.subjectId.isNotBlank()
        ) {
            Text(stringResource(R.string.continue_action))
        }
    }

    if (uiState.showBaselineChoice) {
        BaselineChoiceDialog(
            onDismiss = onDismissBaselineChoice,
            onUseExisting = onUseExistingBaseline,
            onCreateNew = onCreateBaseline
        )
    }
}

@Composable
private fun BaselineChoiceDialog(
    onDismiss: () -> Unit,
    onUseExisting: () -> Unit,
    onCreateNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.baseline_choice_title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.baseline_choice_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onUseExisting) {
                    Text(
                        text = stringResource(R.string.use_existing_baseline),
                        textAlign = TextAlign.Center
                    )
                }
                Button(onClick = onCreateNew) {
                    Text(
                        text = stringResource(R.string.create_new_baseline),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SubjectSetupScreenPreview() {
    BioWatchTheme {
        SubjectSetupScreen(
            uiState = SubjectSetupUiState(
                subjectId = "subject_01",
                isLoading = false,
                hasMatchingBaseline = true
            ),
            onSubjectIdChange = {},
            onContinue = {},
            onUseExistingBaseline = {},
            onCreateBaseline = {},
            onDismissBaselineChoice = {}
        )
    }
}
