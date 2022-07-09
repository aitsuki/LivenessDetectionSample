package com.aitsuki.liveness.tasks

import android.graphics.PointF
import android.util.Log
import com.aitsuki.liveness.LivenessAnalyzer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class MouthDetectionTask(private val detectionTime: Long = 2000) : LivenessAnalyzer.DetectionTask {

    private var time: Long = 0L

    // https://stackoverflow.com/questions/42107466/android-mobile-vision-api-detect-mouth-is-open
    override fun process(face: Face): Boolean {
        if (time == 0L) {
            time = System.currentTimeMillis()
        }
        val left = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return false
        val right = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return false
        val bottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        // Square of lengths be a2, b2, c2
        val a2 = lengthSquare(right, bottom)
        val b2 = lengthSquare(left, bottom)
        val c2 = lengthSquare(left, right)

        // length of sides be a, b, c
        val a = sqrt(a2)
        val b = sqrt(b2)
        val c = sqrt(c2)

        // From Cosine law
        val alpha = acos((b2 + c2 - a2) / (2 * b * c))
        val beta = acos((a2 + c2 - b2) / (2 * a * c))
        val gamma = acos((a2 + b2 - c2) / (2 * a * b))

        // Converting to degrees
        val alphaDeg = alpha * 180 / Math.PI
        val betaDeg = beta * 180 / Math.PI
        val gammaDeg = gamma * 180 / Math.PI

        Log.d(TAG, "OpenMouthTask: alpha: $alphaDeg, beta: $betaDeg, gamma: $gammaDeg")

        if (gammaDeg < 120f) {
            if (System.currentTimeMillis() - time > detectionTime) {
                reset()
                return true
            }
        } else {
            reset()
        }
        return false
    }

    override fun reset() {
        time = 0L
    }

    companion object {

        private const val TAG = "MouthDetectionTask"

        private fun lengthSquare(a: PointF, b: PointF): Float {
            val x = a.x - b.x
            val y = a.y - b.y
            return x * x + y * y
        }
    }
}