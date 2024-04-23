package com.example.liveness

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import com.example.liveness.core.state.DetectionState
import com.example.liveness.core.state.FrontFaceDetectionState
import com.example.liveness.core.state.MouthOpenDetectionState
import com.example.liveness.core.state.SideFaceDetectionState
import com.example.liveness.core.state.SmileDetectionState
import com.example.liveness.ui.component.CameraPreview
import com.example.liveness.ui.component.RequestCameraPermission
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(ExperimentalGetImage::class)
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

    var faceError: FaceDetectionError? by remember { mutableStateOf(null) }
    var statusText by remember { mutableStateOf("") }

    val faceDetectionController = remember {
        val states = listOf(
            FrontFaceDetectionState(),
            SmileDetectionState(),
            SideFaceDetectionState(),
            MouthOpenDetectionState()
        )
        val onFrame = fun(state: DetectionState) {
            faceError = null
            statusText = when (state) {
                is FrontFaceDetectionState -> "Please look at the camera"
                is MouthOpenDetectionState -> "Please open your mouth"
                is SideFaceDetectionState -> "Please show your side face"
                is SmileDetectionState -> "Please smile"
                else -> ""
            }
        }
        val onError = fun(detectionError: FaceDetectionError) {
            faceError = detectionError
            statusText = when (detectionError) {
                FaceDetectionError.NoFace -> "No face detection"
                FaceDetectionError.MultiFace -> "Multiple Face"
            }
        }

        FaceDetectionController(context, states, onFrame, onError) { photos ->
            coroutineScope.launch {
                navController.previousBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
                    savedStateHandle["liveness_images"] =
                        photos.map { it.absolutePath }.toTypedArray()
                }
                navController.popBackStack()
            }
        }
    }

    if (!grantedPermission) {
        RequestCameraPermission(
            onDenied = { navController.popBackStack() },
            onGranted = { grantedPermission = true }
        )
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
                    color = if (faceError != null) colorScheme.error else colorScheme.primary,
                    style = typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CameraPreview(
                    enabled = grantedPermission,
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture = faceDetectionController.imageCapture,
                    imageAnalysis = faceDetectionController.imageAnalysis,
                    modifier = Modifier
                        .size(maxSize)
                        .clip(shape = RoundedCornerShape(50))
                )
            }
        }
    }
}