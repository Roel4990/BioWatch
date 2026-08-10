package com.example.biowatch.presentation.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

private const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
private const val READ_HEALTH_DATA_IN_BACKGROUND =
    "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) {
        READ_HEART_RATE
    } else {
        Manifest.permission.BODY_SENSORS
    }
    val backgroundPermission = when {
        Build.VERSION.SDK_INT >= 36 -> READ_HEALTH_DATA_IN_BACKGROUND
        Build.VERSION.SDK_INT >= 33 -> Manifest.permission.BODY_SENSORS_BACKGROUND
        else -> null
    }
    val notificationPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.startHeartRateTracking()
        } else {
            homeViewModel.onPermissionDenied()
        }
    }
    val heartRatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            homeViewModel.onPermissionDenied()
        } else if (backgroundPermission == null || ContextCompat.checkSelfPermission(
                context,
                backgroundPermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            homeViewModel.startHeartRateTracking()
        } else {
            backgroundPermissionLauncher.launch(backgroundPermission)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        when {
            !isGranted -> homeViewModel.onPermissionDenied()
            ContextCompat.checkSelfPermission(
                context,
                heartRatePermission
            ) != PackageManager.PERMISSION_GRANTED -> {
                heartRatePermissionLauncher.launch(heartRatePermission)
            }

            backgroundPermission != null && ContextCompat.checkSelfPermission(
                context,
                backgroundPermission
            ) != PackageManager.PERMISSION_GRANTED -> {
                backgroundPermissionLauncher.launch(backgroundPermission)
            }

            else -> homeViewModel.startHeartRateTracking()
        }
    }

    fun requestPermissionsAndStart() {
        when {
            notificationPermission != null && ContextCompat.checkSelfPermission(
                context,
                notificationPermission
            ) != PackageManager.PERMISSION_GRANTED -> {
                notificationPermissionLauncher.launch(notificationPermission)
            }

            ContextCompat.checkSelfPermission(
                context,
                heartRatePermission
            ) != PackageManager.PERMISSION_GRANTED -> {
                heartRatePermissionLauncher.launch(heartRatePermission)
            }

            backgroundPermission != null && ContextCompat.checkSelfPermission(
                context,
                backgroundPermission
            ) != PackageManager.PERMISSION_GRANTED -> {
                backgroundPermissionLauncher.launch(backgroundPermission)
            }

            else -> homeViewModel.startHeartRateTracking()
        }
    }

    LaunchedEffect(heartRatePermission, backgroundPermission, notificationPermission) {
        requestPermissionsAndStart()
    }

    HomeScreen(
        uiState = uiState,
        onStartTracking = ::requestPermissionsAndStart,
        onStopTracking = homeViewModel::stopHeartRateTracking
    )
}
