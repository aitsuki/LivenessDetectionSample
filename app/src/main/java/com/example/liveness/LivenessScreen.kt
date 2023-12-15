package com.example.liveness

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.content.ContextCompat
import com.example.liveness.core.DetectionTask
import com.example.liveness.core.FaceAnalyzer
import com.example.liveness.core.LivenessDetector
import com.example.liveness.core.tasks.FacingDetectionTask
import com.example.liveness.core.tasks.MouthOpenDetectionTask
import com.example.liveness.core.tasks.ShakeDetectionTask
import com.example.liveness.core.tasks.SmileDetectionTask
import com.example.liveness.ui.component.CameraPreview
import com.example.liveness.ui.component.RequestCameraPermission
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivenessScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    var grantedPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, CAMERA) == PERMISSION_GRANTED
        )
    }

    if (!grantedPermission) {
        RequestCameraPermission(
            onDenied = { navController.popBackStack() },
            onGranted = { grantedPermission = true }
        )
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var statusText by remember { mutableStateOf("") }
    val imageAnalysis = remember {
        val listener = object : LivenessDetector.Listener {
            val faceImages = arrayListOf<File>()

            fun takePicture(onCompleted: (File) -> Unit = {}) {
                val outputFile = File.createTempFile("face_", ".jpg", context.cacheDir)
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(outputFile).build(),
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onCompleted(outputFile)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("Liveness", "take picture error: ${exception.message}", exception)
                        }
                    })
            }

            @SuppressLint("SetTextI18n")
            override fun onTaskStarted(task: DetectionTask) {
                when (task) {
                    is FacingDetectionTask ->
                        statusText = "Please squarely facing the camera"

                    is ShakeDetectionTask ->
                        statusText = "Slowly shake your head left or right"

                    is MouthOpenDetectionTask ->
                        statusText = "Please open your mouth"

                    is SmileDetectionTask ->
                        statusText = "Please smile"
                }
            }

            override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean) {
                takePicture { file ->
                    faceImages.add(file)
                    if (isLastTask) {
                        coroutineScope.launch {
                            navController.previousBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
                                savedStateHandle["liveness_images"] =
                                    faceImages.map { it.absolutePath }.toTypedArray()
                            }
                            navController.popBackStack()
                        }
                    }
                }
            }

            override fun onTaskFailed(task: DetectionTask, code: Int) {
            }
        }

        val livenessDetector = LivenessDetector(
            FacingDetectionTask(),
            ShakeDetectionTask(),
            MouthOpenDetectionTask(),
            SmileDetectionTask()
        ).also { it.setListener(listener) }

        ImageAnalysis.Builder().build()
            .apply { setAnalyzer(cameraExecutor, FaceAnalyzer(livenessDetector)) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Liveness") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var maxSize = min(272.dp, maxWidth * 0.72f)
            maxSize = min(maxSize, maxHeight - 48.dp)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = statusText,
                    color = colorScheme.primary,
                    style = typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CameraPreview(
                    enabled = grantedPermission,
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture = imageCapture,
                    imageAnalysis = imageAnalysis,
                    modifier = Modifier
                        .size(maxSize)
                        .clip(shape = RoundedCornerShape(50))
                )
            }
        }
    }
}