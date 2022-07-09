package com.aitsuki.liveness

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aitsuki.liveness.databinding.ActivityLivenessDetectionBinding
import com.aitsuki.liveness.tasks.FacingDetectionTask
import com.aitsuki.liveness.tasks.MouthDetectionTask
import com.aitsuki.liveness.tasks.ShakeDetectionTask
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val TAG = "LivenessDetection"

class LivenessDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessDetectionBinding
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivenessDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Add a circle mask to the camera preview
        binding.cameraPreview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
        binding.cameraPreview.clipToOutline = true
    }

    @SuppressLint("SetTextI18n")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Image analysis
            val imageAnalysis = ImageAnalysis.Builder().build()
            imageAnalysis.setAnalyzer(
                cameraExecutor,
                LivenessAnalyzer(
                    this, listOf(
                        FacingDetectionTask(),
                        ShakeDetectionTask(),
                        MouthDetectionTask()
                    ), taskCallback
                )
            )

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private val taskCallback = object : LivenessAnalyzer.TaskCallback() {

        @SuppressLint("SetTextI18n")
        override fun onTaskStart(task: LivenessAnalyzer.DetectionTask) {
            when (task) {
                is FacingDetectionTask -> {
                    binding.guide.text = "Please facing in the camera"
                }
                is ShakeDetectionTask -> {
                    binding.guide.text = "Please shake your head"
                }
                is MouthDetectionTask -> {
                    binding.guide.text = "Please open your mouth"
                }
            }
        }

        override fun onTaskFinish(task: LivenessAnalyzer.DetectionTask) {
            when (task) {
                is FacingDetectionTask -> {
                    takePhoto(File(filesDir, "facing_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_FACING_CAMERA_IMAGE_PATH, it.absolutePath)
                    }
                }
                is ShakeDetectionTask -> {
                    takePhoto(File(filesDir, "shake_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_SHAKE_IMAGE_PATH, it.absolutePath)
                    }
                }
                is MouthDetectionTask -> {
                    takePhoto(File(filesDir, "mouth_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_OPEN_MOUTH_IMAGE_PATH, it.absolutePath)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun takePhoto(file: File, onSaved: (File) -> Unit = {}) {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo capture succeeded: ${output.savedUri}")
                    onSaved(file)
                }
            }
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val RESULT_KEY_FACING_CAMERA_IMAGE_PATH = "facing_camera"
        const val RESULT_KEY_SHAKE_IMAGE_PATH = "shake"
        const val RESULT_KEY_OPEN_MOUTH_IMAGE_PATH = "open_mouth"

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}