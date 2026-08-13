package com.example.biowatch.presentation.screen.startup

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
fun StartupHealthScreen(
    uiState: StartupHealthUiState,
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
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (uiState.status) {
            StartupHealthStatus.CHECKING,
            StartupHealthStatus.READY -> {
                ServerPulse()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        if (uiState.status == StartupHealthStatus.CHECKING) {
                            R.string.server_checking
                        } else {
                            R.string.server_connected
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            StartupHealthStatus.ERROR -> {
                Text(
                    text = stringResource(R.string.server_connection_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry_server_connection))
                }
            }
        }
    }
}

@Composable
private fun ServerPulse() {
    val transition = rememberInfiniteTransition(label = "serverPulse")
    val scale by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "serverPulseScale"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun StartupHealthScreenPreview() {
    BioWatchTheme {
        StartupHealthScreen(
            uiState = StartupHealthUiState(status = StartupHealthStatus.ERROR),
            onRetry = {}
        )
    }
}
