package com.aitsuki.liveness.sample.view

import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aitsuki.liveness.FaceDetectionController
import com.aitsuki.liveness.FaceDetectionError
import com.aitsuki.liveness.sample.view.databinding.ActivityLivenessBinding
import com.aitsuki.liveness.state.FrontFaceDetectionState
import com.aitsuki.liveness.state.MouthOpenDetectionState
import com.aitsuki.liveness.state.SideFaceDetectionState
import com.aitsuki.liveness.state.SmileDetectionState

class LivenessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessBinding

    private val faceController by lazy {
        FaceDetectionController(
            context = this,
            states = listOf(
                FrontFaceDetectionState(),
                SmileDetectionState(),
                SideFaceDetectionState(),
                MouthOpenDetectionState()
            ),
            onFrame = { state ->
                val statusText = when (state) {
                    is FrontFaceDetectionState -> getString(com.aitsuki.liveness.R.string.face_tips_front_face)
                    is MouthOpenDetectionState -> getString(com.aitsuki.liveness.R.string.face_tips_mouth)
                    is SideFaceDetectionState -> getString(com.aitsuki.liveness.R.string.face_tips_side_face)
                    is SmileDetectionState -> getString(com.aitsuki.liveness.R.string.face_tips_smile)
                    else -> ""
                }
                binding.status.setTextColor(Color.BLACK)
                binding.status.text = statusText
            },
            onError = { error ->
                val statusText = when (error) {
                    FaceDetectionError.NoFace -> getString(com.aitsuki.liveness.R.string.error_no_faces)
                    FaceDetectionError.MultiFace -> getString(com.aitsuki.liveness.R.string.error_multi_faces)
                }
                binding.status.setTextColor(Color.RED)
                binding.status.text = statusText
            },
            onCompleted = { files ->
                val paths = files.map { it.absolutePath }.toTypedArray()
                setResult(RESULT_OK, Intent().putExtra("liveness_images", paths))
                finish()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLivenessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.preview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.5f)
            }
        }
        binding.preview.clipToOutline = true
        CameraPermissionHelper(this).request { startCamera() }
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).addListener({
            val cameraProvider = ProcessCameraProvider.getInstance(this).get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.preview.surfaceProvider)
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    faceController.imageAnalysis,
                    faceController.imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}