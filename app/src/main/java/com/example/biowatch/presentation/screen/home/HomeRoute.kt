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
private const val READ_ADDITIONAL_HEALTH_DATA =
    "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA"

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onOpenCollection: () -> Unit = {}
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

    val activityRecognitionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.startHeartRateTracking()
        } else {
            homeViewModel.onPermissionDenied()
        }
    }

    val additionalHealthPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            homeViewModel.onPermissionDenied()
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            homeViewModel.startHeartRateTracking()
        } else {
            activityRecognitionPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            homeViewModel.onPermissionDenied()
        } else if (ContextCompat.checkSelfPermission(
                context,
                READ_ADDITIONAL_HEALTH_DATA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            additionalHealthPermissionLauncher.launch(READ_ADDITIONAL_HEALTH_DATA)
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityRecognitionPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            homeViewModel.startHeartRateTracking()
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
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_ADDITIONAL_HEALTH_DATA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                additionalHealthPermissionLauncher.launch(READ_ADDITIONAL_HEALTH_DATA)
            } else if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            } else {
                homeViewModel.startHeartRateTracking()
            }
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

            ContextCompat.checkSelfPermission(
                context,
                READ_ADDITIONAL_HEALTH_DATA
            ) != PackageManager.PERMISSION_GRANTED -> {
                additionalHealthPermissionLauncher.launch(READ_ADDITIONAL_HEALTH_DATA)
            }

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED -> {
                activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
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

            ContextCompat.checkSelfPermission(
                context,
                READ_ADDITIONAL_HEALTH_DATA
            ) != PackageManager.PERMISSION_GRANTED -> {
                additionalHealthPermissionLauncher.launch(READ_ADDITIONAL_HEALTH_DATA)
            }

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED -> {
                activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
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
        onStopTracking = homeViewModel::stopHeartRateTracking,
        onOpenCollection = onOpenCollection
    )
}
