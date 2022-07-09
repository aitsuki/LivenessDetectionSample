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
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.aitsuki.liveness.databinding.ActivityLivenessDetectionBinding
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val TAG = "LivenessDetection"

class LivenessDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessDetectionBinding
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var livenessDetectionFlow: LivenessDetectionFlow? = null
    private var faceDetector: FaceDetector? = null

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
            val faceDetectionOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .enableTracking()
                .setMinFaceSize(0.5f)
                .build()
            faceDetector = FaceDetection.getClient(faceDetectionOptions)

            val mlKitAnalyzer = MlKitAnalyzer(
                listOf(faceDetector),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                ContextCompat.getMainExecutor(this),
                livenessDetectionConsumer
            )
            val imageAnalyzer = ImageAnalysis.Builder().build()
            imageAnalyzer.setAnalyzer(cameraExecutor, mlKitAnalyzer)


            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private val flowListener = object : LivenessDetectionFlow.Listener {
        @SuppressLint("SetTextI18n")
        override fun onTaskStart(task: LivenessDetectionFlow.Task) {
            when (task) {
                is LivenessDetectionFlow.FacingCameraTask -> {
                    binding.guide.text = "Please facing in the camera"
                }
                is LivenessDetectionFlow.ShakeTask -> {
                    binding.guide.text = "Please shake your head"
                }
                is LivenessDetectionFlow.OpenMouthTask -> {
                    binding.guide.text = "Please open your mouth"
                }
            }
        }

        override fun onTaskFinish(task: LivenessDetectionFlow.Task) {
            when (task) {
                is LivenessDetectionFlow.FacingCameraTask -> {
                    Log.d(TAG, "detect facing camera success")
                    takePhoto(File(filesDir, "facing_camera_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_FACING_CAMERA_IMAGE_PATH, it.absolutePath)
                    }
                }
                is LivenessDetectionFlow.ShakeTask -> {
                    Log.d(TAG, "detect shake success")
                    takePhoto(File(filesDir, "shake_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_SHAKE_IMAGE_PATH, it.absolutePath)
                    }
                }
                is LivenessDetectionFlow.OpenMouthTask -> {
                    Log.d(TAG, "detect open mouth success")
                    takePhoto(File(filesDir, "open_mouth_${System.currentTimeMillis()}.jpg")) {
                        intent.putExtra(RESULT_KEY_OPEN_MOUTH_IMAGE_PATH, it.absolutePath)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
            }
        }
    }

    private val livenessDetectionConsumer = Consumer<MlKitAnalyzer.Result> {
        val face = it.getValue(faceDetector!!)?.firstOrNull() ?: return@Consumer
        if (livenessDetectionFlow == null) {
            livenessDetectionFlow = LivenessDetectionFlow(
                listOf(
                    LivenessDetectionFlow.FacingCameraTask(),
                    LivenessDetectionFlow.ShakeTask(),
                    LivenessDetectionFlow.OpenMouthTask()
                ), flowListener
            )
        }

        if (livenessDetectionFlow != null) {
            when (livenessDetectionFlow!!.detect(face)) {
                LivenessDetectionFlow.Status.Failed -> {
                    livenessDetectionFlow = null
                    Log.d(TAG, "startCamera: liveness detection failed")
                }
                LivenessDetectionFlow.Status.Success -> {
                    livenessDetectionFlow = null
                    Log.d(TAG, "startCamera: liveness detection success")
                    Toast.makeText(this, "Liveness detection success", Toast.LENGTH_SHORT).show()
                }
                else -> {}
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