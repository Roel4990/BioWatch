package com.example.biowatch.presentation.screen.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.biowatch.R

@Composable
fun PermissionSetupRoute(
    onPermissionsReady: () -> Unit,
    viewModel: PermissionSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val requiredPermissions = remember { requiredPermissions(context) }
    var pendingPermissions by remember { mutableStateOf(emptyList<RequiredPermission>()) }

    fun missingPermissions(): List<RequiredPermission> = requiredPermissions.filter { item ->
        ContextCompat.checkSelfPermission(context, item.permission) !=
            PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val requested = pendingPermissions.firstOrNull()
        if (!isGranted && requested != null) {
            val missing = missingPermissions()
            pendingPermissions = emptyList()
            viewModel.onPermissionDenied(
                label = requested.label,
                remainingLabels = missing.map(RequiredPermission::label)
            )
        } else {
            pendingPermissions = pendingPermissions.drop(1)
            if (pendingPermissions.isEmpty()) {
                viewModel.updateMissingPermissions(
                    missingPermissions().map(RequiredPermission::label)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateMissingPermissions(missingPermissions().map(RequiredPermission::label))
    }

    LaunchedEffect(pendingPermissions.firstOrNull()?.permission, uiState.status) {
        if (uiState.status == PermissionSetupStatus.REQUESTING) {
            pendingPermissions.firstOrNull()?.let { permissionLauncher.launch(it.permission) }
        }
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == PermissionSetupStatus.GRANTED) {
            onPermissionsReady()
        }
    }

    PermissionSetupScreen(
        uiState = uiState,
        onRequestPermissions = {
            val missing = missingPermissions()
            if (missing.isEmpty()) {
                viewModel.updateMissingPermissions(emptyList())
            } else {
                viewModel.startRequest(missing.map(RequiredPermission::label))
                pendingPermissions = missing
            }
        }
    )
}

private fun requiredPermissions(context: Context): List<RequiredPermission> = buildList {
    if (Build.VERSION.SDK_INT >= 33) {
        add(RequiredPermission(Manifest.permission.POST_NOTIFICATIONS, context.getString(R.string.permission_notifications)))
    }
    add(
        RequiredPermission(
            permission = if (Build.VERSION.SDK_INT >= 36) READ_HEART_RATE
                else Manifest.permission.BODY_SENSORS,
            label = context.getString(R.string.permission_heart_rate)
        )
    )
    when {
        Build.VERSION.SDK_INT >= 36 -> add(
            RequiredPermission(
                READ_HEALTH_DATA_IN_BACKGROUND,
                context.getString(R.string.permission_background_health)
            )
        )
        Build.VERSION.SDK_INT >= 33 -> add(
            RequiredPermission(
                Manifest.permission.BODY_SENSORS_BACKGROUND,
                context.getString(R.string.permission_background_health)
            )
        )
    }
    add(
        RequiredPermission(
            READ_ADDITIONAL_HEALTH_DATA,
            context.getString(R.string.permission_samsung_sensor)
        )
    )
    add(
        RequiredPermission(
            Manifest.permission.ACTIVITY_RECOGNITION,
            context.getString(R.string.permission_activity_recognition)
        )
    )
}

private data class RequiredPermission(val permission: String, val label: String)

private const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
private const val READ_HEALTH_DATA_IN_BACKGROUND =
    "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
private const val READ_ADDITIONAL_HEALTH_DATA =
    "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA"
