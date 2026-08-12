package com.example.biowatch.presentation.screen.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.biowatch.R
import com.example.biowatch.presentation.theme.BioWatchTheme

@Composable
fun PermissionSetupScreen(
    uiState: PermissionSetupUiState,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_setup_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        Text(
            text = stringResource(R.string.permission_setup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        when (uiState.status) {
            PermissionSetupStatus.CHECKING,
            PermissionSetupStatus.GRANTED -> {
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Text(
                    text = stringResource(
                        if (uiState.status == PermissionSetupStatus.CHECKING) {
                            R.string.permission_checking
                        } else {
                            R.string.permission_all_granted
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            PermissionSetupStatus.REQUESTING -> {
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Text(
                    text = stringResource(R.string.permission_requesting),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            PermissionSetupStatus.REQUIRED,
            PermissionSetupStatus.DENIED -> {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = uiState.missingPermissionLabels.joinToString(separator = " · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                uiState.message?.let { message ->
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Button(onClick = onRequestPermissions) {
                    Text(
                        text = stringResource(R.string.allow_permissions),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun PermissionSetupScreenPreview() {
    BioWatchTheme {
        PermissionSetupScreen(
            uiState = PermissionSetupUiState(
                status = PermissionSetupStatus.REQUIRED,
                missingPermissionLabels = listOf("심박수", "백그라운드 건강 데이터")
            ),
            onRequestPermissions = {}
        )
    }
}
