package com.example.liveness.core

import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor
import kotlin.math.max
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

    private var imageWidth = 0
    private var imageHeight = 0
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
        imageWidth = image.height
        imageHeight = image.width
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
        // 1. 检测人脸是否过小或过大
        val faceMaxSize = imageWidth * 0.9f
        val faceMinSize = imageWidth * 0.38f
        val faceSize = face.boundingBox.width()
        if (faceSize > faceMaxSize || faceSize < faceMinSize) return false

        // 2. 人脸区域是否在检测区域
        val verticalSpace = ((imageHeight - imageWidth) * 0.5f).roundToInt()
        detectionRect.set(
            /* left = */ 0,
            /* top = */ verticalSpace,
            /* right = */ imageWidth,
            /* bottom = */ imageHeight - verticalSpace
        )
        // 2.1 人脸区域完全在检测区域中
        if (detectionRect.contains(face.boundingBox)) {
            return true
        }
        // 2.2 人脸区域和检测区域重叠，计算重叠面积至少为人脸区域的 90%
        val faceArea = faceSize * faceSize
        if (overlapArea(detectionRect, face.boundingBox).toFloat() / faceArea >= 0.95f) {
            return true
        }
        return false
    }

    private fun overlapArea(a: Rect, b: Rect): Int {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)
        val width = right - left
        val height = bottom - top
        return (width * height).coerceAtLeast(1)
    }


    enum class Error {
        NO_FACE, MULTI_FACE, OUT_OF_REGION
    }
}