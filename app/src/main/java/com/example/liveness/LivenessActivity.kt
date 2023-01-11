package com.example.liveness

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.example.liveness.core.*
import com.example.liveness.core.tasks.FacingDetectionTask
import com.example.liveness.core.tasks.MouthOpenDetectionTask
import com.example.liveness.core.tasks.ShakeDetectionTask
import com.example.liveness.core.tasks.SmileDetectionTask
import com.example.liveness.databinding.ActivityLivenessBinding
import java.io.File

class LivenessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessBinding
    private lateinit var cameraController: LifecycleCameraController
    private var imageFiles = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivenessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission deny", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.launch(Manifest.permission.CAMERA)

        binding.cameraPreview.clipToOutline = true
        binding.cameraPreview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            FaceAnalyzer(buildLivenessDetector())
        )
        cameraController.bindToLifecycle(this)
        binding.cameraPreview.controller = cameraController
    }

    private fun buildLivenessDetector(): LivenessDetector {
        val listener = object : LivenessDetector.Listener {
            @SuppressLint("SetTextI18n")
            override fun onTaskStarted(task: DetectionTask) {
                when (task) {
                    is FacingDetectionTask ->
                        binding.guide.text = "Please squarely facing the camera"
                    is ShakeDetectionTask ->
                        binding.guide.text = "Slowly shake your head left or right"
                    is MouthOpenDetectionTask ->
                        binding.guide.text = "Please open your mouth"
                    is SmileDetectionTask ->
                        binding.guide.text = "Please smile"
                }
            }

            override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean) {
                takePhoto(File(cacheDir, "${System.currentTimeMillis()}.jpg")) {
                    imageFiles.add(it.absolutePath)
                    if (isLastTask) {
                        finishForResult()
                    }
                }
            }

            override fun onTaskFailed(task: DetectionTask, code: Int) {
                if (code == LivenessDetector.ERROR_MULTI_FACES) {
                    Toast.makeText(
                        this@LivenessActivity,
                        "Please make sure there is only one face on the screen.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        return LivenessDetector(
            FacingDetectionTask(),
            ShakeDetectionTask(),
            MouthOpenDetectionTask(),
            SmileDetectionTask()
        ).also { it.setListener(listener) }
    }

    private fun finishForResult() {
        val result = ArrayList(imageFiles.takeLast(4))
        setResult(RESULT_OK, Intent().putStringArrayListExtra(ResultContract.RESULT_KEY, result))
        finish()
    }


    private fun takePhoto(file: File, onSaved: (File) -> Unit) {
        cameraController.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    e.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }
            }
        )
    }

    class ResultContract : ActivityResultContract<Any?, List<String>?>() {

        companion object {
            const val RESULT_KEY = "images"
        }

        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(context, LivenessActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
            if (resultCode == RESULT_OK && intent != null) {
                return intent.getStringArrayListExtra(RESULT_KEY)
            }
            return null
        }
    }
}