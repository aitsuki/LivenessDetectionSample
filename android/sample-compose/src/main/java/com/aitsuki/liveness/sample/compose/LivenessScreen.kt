package com.aitsuki.liveness.sample.compose

import android.Manifest
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
import com.aitsuki.liveness.FaceDetectionController
import com.aitsuki.liveness.FaceDetectionError
import com.aitsuki.liveness.sample.compose.component.CameraPreview
import com.aitsuki.liveness.sample.compose.component.WithPermission
import com.aitsuki.liveness.state.DetectionState
import com.aitsuki.liveness.state.FrontFaceDetectionState
import com.aitsuki.liveness.state.MouthOpenDetectionState
import com.aitsuki.liveness.state.SideFaceDetectionState
import com.aitsuki.liveness.state.SmileDetectionState
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivenessScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
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
                is FrontFaceDetectionState -> context.getString(com.aitsuki.liveness.R.string.face_tips_front_face)
                is MouthOpenDetectionState -> context.getString(com.aitsuki.liveness.R.string.face_tips_mouth)
                is SideFaceDetectionState -> context.getString(com.aitsuki.liveness.R.string.face_tips_side_face)
                is SmileDetectionState -> context.getString(com.aitsuki.liveness.R.string.face_tips_smile)
                else -> ""
            }
        }
        val onError = fun(detectionError: FaceDetectionError) {
            faceError = detectionError
            statusText = when (detectionError) {
                FaceDetectionError.NoFace -> context.getString(com.aitsuki.liveness.R.string.error_no_faces)
                FaceDetectionError.MultiFace -> context.getString(com.aitsuki.liveness.R.string.error_multi_faces)
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
                WithPermission(
                    permission = Manifest.permission.CAMERA,
                    onDenied = { navController.popBackStack() },
                ) { isGranted ->
                    CameraPreview(
                        enabled = isGranted,
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
}