package com.example.biowatch.presentation.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) {
        "android.permission.health.READ_HEART_RATE"
    } else {
        Manifest.permission.BODY_SENSORS
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.startHeartRateTracking()
        } else {
            homeViewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(heartRatePermission) {
        if (ContextCompat.checkSelfPermission(
                context,
                heartRatePermission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            homeViewModel.startHeartRateTracking()
        } else {
            permissionLauncher.launch(heartRatePermission)
        }
    }

    DisposableEffect(Unit) {
        onDispose(homeViewModel::stopHeartRateTracking)
    }

    HomeScreen(uiState = uiState)
}
