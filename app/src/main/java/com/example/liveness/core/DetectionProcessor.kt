package com.example.liveness.core

import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.*

class DetectionProcessor {

    companion object {
        private const val FACE_CACHE = 3
    }

    val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.7f)
            .build()
    )

    private var errorState = ErrorState.NO_FACE
    private val lastFaces: Deque<Face> = LinkedList()
    private var listener: Listener? = null

    fun process(result: MlKitAnalyzer.Result) {
        val face = filter(result)
        if (face == null) {
            // 重置流程
            return
        }
    }

    private fun filter(result: MlKitAnalyzer.Result): Face? {
        val faces = result.getValue(faceDetector)
        if (faces.isNullOrEmpty()) {
            val lastFace = lastFaces.pollFirst()
            if (lastFace == null) {
                changeErrorState(ErrorState.NO_FACE)
                return null
            }
        }


        if (faces != null && faces.size > 1) {
            changeErrorState(ErrorState.MULTI_FACES)
            return null
        }




        val lastFace = lastFaces[0]
        if (faces.isNullOrEmpty() && lastFace == null) {
            listener?.onDetectNoFace()
            return null
        }


        // pop latest face (index 0)
        System.arraycopy(lastFaces, 0, lastFaces, 1, lastFaces.size - 1)
    }

    private fun changeErrorState(newState: ErrorState) {
        if (newState != errorState) {
            errorState = newState
            when(errorState) {
                ErrorState.NO_FACE -> listener?.onNoFaceDetected()
                ErrorState.MULTI_FACES -> listener?.onMultipleFaceDetected()
                else -> {}
            }

            if (errorState != ErrorState.MULTI_FACES) {
                errorState = ErrorState.MULTI_FACES
                listener?.onMultipleFaceDetected()
            }
        }
    }

    private enum class ErrorState {
        NONE, NO_FACE, MULTI_FACES
    }

    interface Listener {
        fun onNoFaceDetected()

        fun onMultipleFaceDetected()
    }
}