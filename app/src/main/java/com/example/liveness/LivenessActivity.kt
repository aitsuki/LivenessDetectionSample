package com.example.liveness

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.os.Bundle
import android.util.Log
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
import com.example.liveness.databinding.ActivityLivenessBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LivenessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessBinding
    private val analyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraController: LifecycleCameraController
    private var imageFiles: ArrayList<String> = arrayListOf()

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
        cameraController.setImageAnalysisAnalyzer(analyzerExecutor, buildLivenessAnalyzer())
        cameraController.bindToLifecycle(this)
        binding.cameraPreview.controller = cameraController
    }

    private fun buildLivenessAnalyzer(): FaceAnalyzer {
        val items = setOf(
            FacingDetectionItem(),
            ShakeDetectionItem(),
            MouthOpenDetectionItem(),
            SmileDetectionItem()
        )
        return FaceAnalyzer(
            executor = ContextCompat.getMainExecutor(this),
            items = items,
            onFailed = { err ->
                imageFiles.clear()
                if (err == FaceAnalyzer.Error.MULTI_FACE) {
                    Toast.makeText(
                        this,
                        "Please make sure there is only one face on the screen.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onItemStarted = { item ->
                when (item) {
                    is FacingDetectionItem ->
                        binding.guide.text = "Please squarely facing the camera"
                    is ShakeDetectionItem ->
                        binding.guide.text = "Slowly shake your head left or right"
                    is MouthOpenDetectionItem ->
                        binding.guide.text = "Please open your mouth"
                    is SmileDetectionItem ->
                        binding.guide.text = "Please smile"
                }
            },
            onItemPassed = { item ->
                Log.d("onItemPassed", item::class.java.name)
                takePhoto(File(cacheDir, "${System.currentTimeMillis()}.jpg")) {
                    imageFiles.add(it.absolutePath)
                    if (item == items.last()) {
                        finishForResult()
                    }
                }
            }
        )
    }

    private fun finishForResult() {
        setResult(RESULT_OK, Intent().putStringArrayListExtra("images", imageFiles))
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

    override fun onDestroy() {
        super.onDestroy()
        analyzerExecutor.shutdown()
    }

    class ResultContract : ActivityResultContract<Intent, List<String>?>() {

        override fun createIntent(context: Context, input: Intent): Intent {
            return Intent(context, LivenessActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
            if (resultCode == RESULT_OK && intent != null) {
                return intent.getStringArrayListExtra("images")
            }
            return null
        }
    }
}