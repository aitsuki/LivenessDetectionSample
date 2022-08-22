package com.example.liveness

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.liveness.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val livenessLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val data = it.data ?: return@registerForActivityResult
                    val facingImagePath =
                        data.getStringExtra(LivenessActivity.RESULT_KEY_FACING_CAMERA_IMAGE_PATH)
                    val smilingImagePath =
                        data.getStringExtra(LivenessActivity.RESULT_KEY_SMILING_IMAGE_PATH)
                    val mouthImagePath =
                        data.getStringExtra(LivenessActivity.RESULT_KEY_MOUTH_IMAGE_PATH)
                    val shakeImagePath =
                        data.getStringExtra(LivenessActivity.RESULT_KEY_SHAKE_IMAGE_PATH)
                    if (facingImagePath.isNullOrEmpty()
                        || smilingImagePath.isNullOrEmpty()
                        || mouthImagePath.isNullOrEmpty()
                        || shakeImagePath.isNullOrEmpty()
                    ) {
                        Log.d("MainActivity", "face image path error!")
                        Toast.makeText(this, "Face recognize failed.", Toast.LENGTH_SHORT).show()
                        return@registerForActivityResult
                    }
                    Glide.with(this).load(File(facingImagePath)).into(binding.facingImage)
                    Glide.with(this).load(File(smilingImagePath)).into(binding.smilingImage)
                    Glide.with(this).load(File(mouthImagePath)).into(binding.mouthImage)
                    Glide.with(this).load(File(shakeImagePath)).into(binding.shakeImage)
                }
            }

        binding.startBtn.setOnClickListener {
            livenessLauncher.launch(Intent(this, LivenessActivity::class.java))
        }
    }

}