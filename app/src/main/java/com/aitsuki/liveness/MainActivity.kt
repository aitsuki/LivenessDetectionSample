package com.aitsuki.liveness

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.aitsuki.liveness.LivenessDetectionActivity.Companion.RESULT_KEY_FACING_CAMERA_IMAGE_PATH
import com.aitsuki.liveness.LivenessDetectionActivity.Companion.RESULT_KEY_OPEN_MOUTH_IMAGE_PATH
import com.aitsuki.liveness.LivenessDetectionActivity.Companion.RESULT_KEY_SHAKE_IMAGE_PATH
import com.aitsuki.liveness.databinding.ActivityLivenessDetectionBinding
import com.aitsuki.liveness.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val data = it.data ?: return@registerForActivityResult
                    data.getStringExtra(RESULT_KEY_FACING_CAMERA_IMAGE_PATH)?.let { path ->
                        Log.d(TAG, "onCreate: facing camera image path: $path")
                        binding.facingCameraImage.isVisible = true
                        Glide.with(this)
                            .load(File(path))
                            .into(binding.facingCameraImage)
                    }
                    data.getStringExtra(RESULT_KEY_SHAKE_IMAGE_PATH)?.let { path ->
                        Log.d(TAG, "onCreate: shake image path: $path")
                        binding.shakeHeadImage.isVisible = true
                        Glide.with(this)
                            .load(File(path))
                            .into(binding.shakeHeadImage)
                    }
                    data.getStringExtra(RESULT_KEY_OPEN_MOUTH_IMAGE_PATH)?.let { path ->
                        Log.d(TAG, "onCreate: open mouth image path: $path")
                        binding.openMouthImage.isVisible = true
                        Glide.with(this)
                            .load(File(path))
                            .into(binding.openMouthImage)
                    }
                }
            }

        binding.startBtn.setOnClickListener {
            resultLauncher.launch(
                Intent(this, LivenessDetectionActivity::class.java)
            )
        }
    }
}