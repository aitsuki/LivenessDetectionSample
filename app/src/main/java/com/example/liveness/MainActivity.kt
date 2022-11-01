package com.example.liveness

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.liveness.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val livenessLauncher = registerForActivityResult(LivenessActivity.ResultContract()) {
            binding.recyclerView.adapter = ImageAdapter(it.orEmpty())
        }

        binding.startBtn.setOnClickListener {
            livenessLauncher.launch(Intent(this, LivenessActivity::class.java))
        }
    }
}

class ImageAdapter(private val images: List<String>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return object : ViewHolder(imageView) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        Glide.with(holder.itemView).load(image).into(holder.itemView as ImageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }
}