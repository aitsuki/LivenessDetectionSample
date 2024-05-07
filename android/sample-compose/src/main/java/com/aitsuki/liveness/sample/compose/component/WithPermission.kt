package com.aitsuki.liveness.sample.compose.component

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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WithPermission(
    permission: String,
    onDenied: () -> Unit,
    content: @Composable (isGranted: Boolean) -> Unit,
) {
    var showSettingDialog by remember { mutableStateOf(false) }
    var callbackKey by remember { mutableIntStateOf(0) }
    val permissionState = rememberPermissionState(permission = permission) {
        callbackKey += 1
    }

    if (showSettingDialog) {
        SettingsDialog(
            permission = permission,
            onDenied = {
                showSettingDialog = false
                onDenied()
            },
            onGranted = {
                showSettingDialog = false
            }
        )
    }

    LaunchedEffect(callbackKey) {
        if (callbackKey == 0) {
            permissionState.launchPermissionRequest()
            return@LaunchedEffect
        }
        val status = permissionState.status
        if (status is PermissionStatus.Denied) {
            if (status.shouldShowRationale) {
                onDenied()
            } else {
                showSettingDialog = true
            }
        }
    }

    content(permissionState.status.isGranted)
}

@Composable
private fun SettingsDialog(
    permission: String,
    onDenied: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val settingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED) {
                onGranted()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDenied,
        title = { Text(text = stringResource(id = com.aitsuki.liveness.R.string.permission_settings_title)) },
        text = { Text(text = stringResource(id = com.aitsuki.liveness.R.string.permission_settings_content)) },
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
                Text(text = stringResource(id = com.aitsuki.liveness.R.string.permission_settings_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDenied) {
                Text(text = stringResource(id = com.aitsuki.liveness.R.string.permission_settings_cancel))
            }
        },
    )
}