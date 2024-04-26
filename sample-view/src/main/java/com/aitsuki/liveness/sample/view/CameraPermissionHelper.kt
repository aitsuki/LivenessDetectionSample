package com.aitsuki.liveness.sample.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CameraPermissionHelper(private val activity: ComponentActivity) {

    private var onGranted: (() -> Unit)? = null

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onGranted?.invoke()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                )
            ) {
                finishWithPermissionDenied()
            } else {
                showPermissionSettings()
            }
        }

    private val settingsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onGranted?.invoke()
            } else {
                finishWithPermissionDenied()
            }
        }

    private fun showPermissionSettings() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permission Requirement")
            .setMessage("We need camera permissions for face authentication. Please go to the application details page to grant camera permissions.")
            .setPositiveButton("Go to setting") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", activity.packageName, null)
                settingsLauncher.launch(intent)
            }
            .setNegativeButton("Cancel") { _, _ -> finishWithPermissionDenied() }
            .setCancelable(false)
            .show()
    }


    private fun finishWithPermissionDenied() {
        Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
        activity.finish()
    }

    fun request(onGranted: () -> Unit) {
        this.onGranted = onGranted
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}