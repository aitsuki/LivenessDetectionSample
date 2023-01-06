package com.example.liveness.core

import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.core.os.ExecutorCompat
import com.google.mlkit.vision.face.Face

class FaceAnalyzer(private val processor: DetectionProcessor) : Analyzer {

    private val faceHistory = Array<Face?>(3) { null }
    private var currentItem: DetectionItem? = null
    private var lastItem: DetectionItem? = null
    private var isCompleted = false

    private val analyzerDelegate = MlKitAnalyzer(
        listOf(processor.faceDetector),
        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
        ExecutorCompat.create(Handler(Looper.getMainLooper())),
        processor::process
    )

    private fun detect(result: MlKitAnalyzer.Result) {
        if (isCompleted || items.isEmpty()) return
        val faces = result.getValue(faceDetector)
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
        analyzerDelegate.analyze(image)
    }

    override fun getDefaultTargetResolution(): Size {
        return analyzerDelegate.defaultTargetResolution
    }

    override fun getTargetCoordinateSystem(): Int {
        return analyzerDelegate.targetCoordinateSystem
    }

    override fun updateTransform(matrix: Matrix?) {
        analyzerDelegate.updateTransform(matrix)
    }

    enum class Error {
        NO_FACE, MULTI_FACE, OUT_OF_REGION
    }
}