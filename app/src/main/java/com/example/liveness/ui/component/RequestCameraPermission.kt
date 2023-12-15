package com.example.liveness.ui.component

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestCameraPermission(
    onDenied: () -> Unit,
    onGranted: () -> Unit
) {
    var showSettingDialog by remember { mutableStateOf(false) }
    var callbackKey by remember { mutableIntStateOf(0) }
    val permissionState = rememberPermissionState(permission = CAMERA) {
        callbackKey += 1
    }

    if (showSettingDialog) {
        SettingsDialog(
            onDenied = {
                showSettingDialog = false
                onDenied()
            },
            onGranted = {
                showSettingDialog = false
                onGranted()
            }
        )
    }

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    LaunchedEffect(callbackKey) {
        if (callbackKey == 0) return@LaunchedEffect
        val status = permissionState.status
        if (status is PermissionStatus.Denied) {
            if (status.shouldShowRationale) {
                onDenied()
            } else {
                showSettingDialog = true
            }
        } else {
            onGranted()
        }
    }
}

@Composable
private fun SettingsDialog(
    onDenied: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val settingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (ContextCompat.checkSelfPermission(context, CAMERA) == PERMISSION_GRANTED) {
                onGranted()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDenied,
        title = { Text(text = "Permission Requirement") },
        text = { Text(text = "We need camera permissions for face authentication. Please go to the application details page to grant camera permissions.") },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts(
                    "package",
                    context.packageName,
                    null
                )
                settingLauncher.launch(intent)
            }) {
                Text(text = "Go to setting")
            }
        },
        dismissButton = {
            TextButton(onClick = onDenied) {
                Text(text = "Cancel")
            }
        },
    )
}