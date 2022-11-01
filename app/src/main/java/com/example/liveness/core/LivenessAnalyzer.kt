package com.example.liveness.core

import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * LivenessAnalyzer 接收连续的人脸数据，通过一系列的检测项分析检测目标时否为活体。当检测到活体后，会回调外部
 * 程序，此后分析器将不再接收人脸数据。
 *
 * @param items 检测项，例如 摇头检测，微笑检测等
 * @param onFailed 活体检测失败，返回失败原因，并自动重启检测流程。（此回调会非常频繁）
 */
class LivenessAnalyzer(
    executor: Executor,
    val items: Set<DetectionItem>,
    private val onFailed: (error: Error) -> Unit,
    private val onItemStarted: (item: DetectionItem) -> Unit,
    private val onItemPassed: (item: DetectionItem) -> Unit,
) : Analyzer {

    private var imageCropWidth = 0
    private var imageCropHeight = 0
    private var imageRotateDegrees = 0
    private val detectionRect = Rect()
    private val faceHistory = Array<Face?>(3) { null }
    private var currentItem: DetectionItem? = null
    private var lastItem: DetectionItem? = null
    private var isCompleted = false

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
    )

    private val proxyAnalyzer = MlKitAnalyzer(
        listOf(faceDetector),
        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
        executor,
        ::detect
    )

    private fun detect(result: MlKitAnalyzer.Result) {
        if (isCompleted || items.isEmpty()) return
        val faces = result.getValue(faceDetector).orEmpty()
        Log.d("detect", "detect: ${faces.size}")
        when {
            faces.size > 1 -> failReset(Error.MULTI_FACE)
            faces.isEmpty() && faceHistory.all { it == null } -> failReset(Error.NO_FACE)
            !isFaceInDetectionRect(faces.firstOrNull()) -> failReset(Error.OUT_OF_REGION)
        }
        // cache the face
        System.arraycopy(faceHistory, 0, faceHistory, 1, faceHistory.size - 1)
        faceHistory[0] = faces.firstOrNull()
        val face = faceHistory[0] ?: return

        // detect flow
        var item = currentItem
        item = item ?: items.first()
        if (item != lastItem) {
            lastItem = item
            item.prepare()
            onItemStarted(item)
        }
        if (item.detect(face)) {
            onItemPassed(item)
            item = getNextItem(item)
        }
        if (item == null) {
            isCompleted = true
        }
        currentItem = item
    }

    private fun getNextItem(item: DetectionItem): DetectionItem? {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == item && iterator.hasNext()) {
                return iterator.next()
            }
        }
        return null
    }

    private fun failReset(error: Error) {
        onFailed(error)
        currentItem = null
        lastItem = null
    }

    override fun analyze(image: ImageProxy) {
        imageCropWidth = image.height
        imageCropHeight = image.width
        imageRotateDegrees = image.imageInfo.rotationDegrees
        proxyAnalyzer.analyze(image)
    }

    override fun getDefaultTargetResolution(): Size {
        return proxyAnalyzer.defaultTargetResolution
    }

    override fun getTargetCoordinateSystem(): Int {
        return proxyAnalyzer.targetCoordinateSystem
    }

    override fun updateTransform(matrix: Matrix?) {
        proxyAnalyzer.updateTransform(matrix)
    }

    private fun isFaceInDetectionRect(face: Face?): Boolean {
        face ?: return false
        var resolutionWidth = imageCropWidth
        var resolutionHeight = imageCropHeight
        if (imageRotateDegrees == 90 || imageRotateDegrees == 270) {
            resolutionWidth = imageCropHeight
            resolutionHeight = imageCropWidth
        }
        val faceX = face.boundingBox.centerX()
        val faceY = face.boundingBox.centerY()
        val faceSize = face.boundingBox.width()
        val maxSize = min(resolutionWidth, resolutionHeight) * 1f
        val minSize = min(resolutionWidth, resolutionHeight) * 0.4f
        if (faceSize > maxSize || faceSize < minSize) return false
        detectionRect.set(
            (resolutionWidth * 0.2f).roundToInt(),
            (resolutionHeight * 0.2f).roundToInt(),
            (resolutionWidth * 0.8f).roundToInt(),
            (resolutionHeight * 0.8f).roundToInt()
        )
        return detectionRect.contains(faceX, faceY)
    }

    enum class Error {
        NO_FACE, MULTI_FACE, OUT_OF_REGION
    }
}