package com.example.biowatch.presentation.screen.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.SystemClock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
fun HomeScreen(
    uiState: HomeUiState,
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var titleTapCount by remember { mutableIntStateOf(0) }
    var lastTitleTapMillis by remember { mutableLongStateOf(0L) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
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

    if (!uiState.isWatchWorn) {
        WatchNotWornContent(
            onStopTracking = onStopTracking,
            onTitleClick = onTitleClick,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onTitleClick)
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
            DashboardValue(
                label = stringResource(R.string.status_label),
                value = stringResource(uiState.status.stringResource)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = if (uiState.isTracking) onStopTracking else onStartTracking) {
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
        }
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
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(onClick = onTitleClick)
        )
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

private const val REQUIRED_TITLE_TAPS = 3
private const val TITLE_TAP_TIMEOUT_MILLIS = 1_500L
