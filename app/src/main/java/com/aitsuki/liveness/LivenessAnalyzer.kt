package com.aitsuki.liveness

import android.content.Context
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class LivenessAnalyzer(
    context: Context,
    private val tasks: List<DetectionTask>,
    private val callback: TaskCallback
) : ImageAnalysis.Analyzer, Consumer<MlKitAnalyzer.Result> {

    private var trackingFaceId: Int? = null
    private var currentTaskIndex = 0
    private val faceDetector = buildDefaultFaceDetector()
    private var stopped = false

    private val mlKitAnalyzer = MlKitAnalyzer(
        listOf(faceDetector),
        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
        ContextCompat.getMainExecutor(context),
        this
    )

    private fun reset() {
        Log.d(TAG, "reset")
        tasks.forEach { it.reset() }
        trackingFaceId = null
        currentTaskIndex = 0
        callback.onResetTasks()
    }

    override fun accept(result: MlKitAnalyzer.Result) {
        if (stopped) return
        val face = result.getValue(faceDetector)?.firstOrNull() ?: return
        if (trackingFaceId == null) {
            trackingFaceId = face.trackingId
            callback.onTaskStart(tasks[currentTaskIndex])
        } else if (trackingFaceId != face.trackingId) {
            reset()
            return
        }

        if (tasks[currentTaskIndex].process(face)) {
            callback.onTaskFinish(tasks[currentTaskIndex])
            currentTaskIndex++
            if (currentTaskIndex < tasks.size) {
                callback.onTaskStart(tasks[currentTaskIndex])
            } else {
                if (callback.onAllTasksFinish()) {
                    stopped = true
                } else {
                    reset()
                }
            }
        }
    }

    override fun analyze(image: ImageProxy) {
        mlKitAnalyzer.analyze(image)
    }

    override fun getTargetResolutionOverride(): Size {
        return mlKitAnalyzer.targetResolutionOverride
    }

    override fun getTargetCoordinateSystem(): Int {
        return mlKitAnalyzer.targetCoordinateSystem
    }

    override fun updateTransform(matrix: Matrix?) {
        return mlKitAnalyzer.updateTransform(matrix)
    }

    abstract class TaskCallback {
        open fun onTaskStart(task: DetectionTask) {}
        open fun onTaskFinish(task: DetectionTask) {}

        /**
         * @return if ture, the analyzer will stop. Otherwise, it will restart the analysis.
         */
        open fun onAllTasksFinish(): Boolean {
            return true
        }

        open fun onResetTasks() {}
    }

    interface DetectionTask {
        /**
         * @return return true if the task is finished, false is need continue.
         */
        fun process(face: Face): Boolean

        /**
         * Reset the task state.
         */
        fun reset()
    }

    companion object {

        private const val TAG = "LivenessAnalyzer"

        fun buildDefaultFaceDetector(): FaceDetector {
            val faceDetectionOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .enableTracking()
                .setMinFaceSize(0.5f)
                .build()
            return FaceDetection.getClient(faceDetectionOptions)
        }
    }
}