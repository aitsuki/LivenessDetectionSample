package com.example.liveness

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.example.liveness.core.DetectionContext
import com.example.liveness.core.DetectionUtils
import com.example.liveness.core.state.DetectionState
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File

enum class FaceDetectionError {
    NoFace, MultiFace
}

@OptIn(ExperimentalGetImage::class)
class FaceDetectionController(
    private val context: Context,
    private val states: List<DetectionState>,
    val onFrame: (DetectionState) -> Unit,
    val onError: (FaceDetectionError) -> Unit,
    val onCompleted: (List<File>) -> Unit,
) {
    private val photos: ArrayList<File> = ArrayList()
    private var imageWidth = 0
    private var imageHeight = 0
    private var needUpdateImageInfo = true
    private var consecutiveEmptyFacesTimes = 0
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.9f)
            .build()
    )


    val imageCapture: ImageCapture = ImageCapture.Builder().build()
    val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder().build()

    private val detectionContext = DetectionContext(
        states = states,
        stateChangeCallback = object : DetectionContext.StateChangeCallback {
            override fun onNext(next: () -> Unit, retry: () -> Unit) {
                takePhoto(onImageSaved = { file ->
                    photos.add(file)
                    next()
                }, onError = { tr ->
                    Log.e(
                        "FaceImageAnalysis",
                        "Failed to take photo. Error: ",
                        tr
                    )
                    retry()
                })
            }
        },
        onCompleteListener = object : DetectionContext.OnCompleteListener {
            override fun onComplete() {
                onCompleted(photos.takeLast(states.size))
            }
        }
    )

    init {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            updateImageInfo(imageProxy)
            try {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            processFaces(faces)
                        }
                        .addOnFailureListener { e ->
                            processFaces(emptyList())
                            Log.e(
                                "FaceImageAnalysis",
                                "Failed to detect image. Error: " + e.localizedMessage
                            )
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            } catch (e: MlKitException) {
                processFaces(emptyList())
                Log.e("FaceImageAnalysis", "Failed to process image. Error: " + e.localizedMessage)
            }
        }
    }

    private fun processFaces(faces: List<Face>) {
        if (faces.size > 1) {
            onError(FaceDetectionError.MultiFace)
            detectionContext.resetState()
        } else if (faces.isEmpty() || !DetectionUtils.isWholeFace(
                faces.first(),
                imageWidth,
                imageHeight
            )
        ) {
            consecutiveEmptyFacesTimes++
            if (consecutiveEmptyFacesTimes >= 5) {
                consecutiveEmptyFacesTimes = 0
                onError(FaceDetectionError.NoFace)
                detectionContext.resetState()
            }
        } else {
            consecutiveEmptyFacesTimes = 0
            onFrame(detectionContext.getCurrentState())
            detectionContext.handle(faces.first())
        }
    }

    private fun takePhoto(
        onImageSaved: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "face${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onImageSaved(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    private fun updateImageInfo(imageProxy: ImageProxy) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0 || rotation == 180) {
            imageWidth = imageProxy.width
            imageHeight = imageProxy.height
            imageProxy.cropRect
        } else {
            imageWidth = imageProxy.height
            imageHeight = imageProxy.width
        }
        needUpdateImageInfo = false
    }
}