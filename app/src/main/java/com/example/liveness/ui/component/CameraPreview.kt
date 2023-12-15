package com.example.liveness.ui.component

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    preview: Preview = remember { Preview.Builder().build() },
    imageCapture: ImageCapture? = null,
    imageAnalysis: ImageAnalysis? = null,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    val cameraProvider by produceState<ProcessCameraProvider?>(initialValue = null) {
        value = ProcessCameraProvider.getInstance(context).await()
    }

    val camera = remember(cameraProvider, enabled) {
        cameraProvider?.let {
            it.unbindAll()
            if (enabled) {
                it.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *listOfNotNull(preview, imageAnalysis, imageCapture).toTypedArray()
                )
            } else {
                null
            }
        }
    }

    LaunchedEffect(camera) {
        camera?.let {
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    KeepScreenOn()
    AndroidView(modifier = modifier, factory = { previewView })
}