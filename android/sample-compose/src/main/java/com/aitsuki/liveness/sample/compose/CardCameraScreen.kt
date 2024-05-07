package com.aitsuki.liveness.sample.compose

import android.Manifest
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.aitsuki.liveness.sample.compose.component.CameraPreview
import com.aitsuki.liveness.sample.compose.component.WithPermission
import java.io.File

@Composable
fun CardCameraScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var previewFile: File? by remember { mutableStateOf(null) }
    var flashOn by remember { mutableStateOf(false) }
    var isShooting by remember { mutableStateOf(false) }

    fun takePicture() {
        isShooting = true
        val outputFile = File.createTempFile("card_", ".jpg", context.cacheDir)
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(outputFile).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isShooting = false
                    previewFile = outputFile
                }

                override fun onError(exception: ImageCaptureException) {
                    isShooting = false
                    Log.e("CardCamera", "take picture error: ${exception.message}", exception)
                }
            })
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.631f) // chinese id card size is 85.6mm x 54mm
                    .border(
                        width = 2.dp,
                        color = Color.Cyan,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clip(shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                WithPermission(
                    permission = Manifest.permission.CAMERA,
                    onDenied = { navController.popBackStack() },
                ) { isGranted ->
                    CameraPreview(
                        enabled = isGranted && previewFile == null,
                        imageCapture = imageCapture,
                    )
                }
                previewFile?.let { file ->
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }

            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.rotateVertically(),
                text = stringResource(id = com.aitsuki.liveness.R.string.camera_tips)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = {
                if (previewFile != null) {
                    previewFile = null
                } else {
                    navController.popBackStack()
                }
            }) {
                Icon(
                    painter = painterResource(
                        if (previewFile != null) {
                            com.aitsuki.liveness.R.drawable.ic_undo
                        } else {
                            com.aitsuki.liveness.R.drawable.ic_close
                        }
                    ),
                    contentDescription = null,
                )
            }

            IconButton(
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                onClick = {
                    if (previewFile != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
                            savedStateHandle["card_image"] = previewFile?.absolutePath
                        }
                        navController.popBackStack()
                    } else {
                        takePicture()
                    }
                },
                enabled = !isShooting,
            ) {
                Icon(
                    painter = painterResource(
                        if (previewFile != null) {
                            com.aitsuki.liveness.R.drawable.ic_camera_ok
                        } else {
                            com.aitsuki.liveness.R.drawable.ic_shoot
                        }
                    ),
                    contentDescription = null
                )
            }

            IconButton(onClick = {
                flashOn = !flashOn
                imageCapture.flashMode =
                    if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            }) {
                Icon(
                    painter = painterResource(
                        id = if (flashOn) {
                            com.aitsuki.liveness.R.drawable.ic_flash_on
                        } else {
                            com.aitsuki.liveness.R.drawable.ic_flash_off
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }
}

fun Modifier.rotateVertically(clockwise: Boolean = true): Modifier {
    val rotate = rotate(if (clockwise) 90f else -90f)

    val adjustBounds = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }
    return rotate then adjustBounds
}