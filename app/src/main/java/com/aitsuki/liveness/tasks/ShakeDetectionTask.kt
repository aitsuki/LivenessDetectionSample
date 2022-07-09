package com.aitsuki.liveness.tasks

import android.util.Log
import com.aitsuki.liveness.LivenessAnalyzer
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

class ShakeDetectionTask(private val detectionTime: Long = 3000) : LivenessAnalyzer.DetectionTask {

    private var shakeLeft = false
    private var shakeRight = false
    private var shakeLeftTime = 0L
    private var shakeRightTime = 0L

    override fun process(face: Face): Boolean {
        Log.d(TAG, "headEulerAngleY: ${face.headEulerAngleY}")

        val yaw = face.headEulerAngleY
        if (yaw > 25f) {
            shakeLeft = true
            shakeLeftTime = System.currentTimeMillis()
        } else if (yaw < -25f) {
            shakeRight = true
            shakeRightTime = System.currentTimeMillis()
        }
        if (shakeLeft && shakeRight) {
            if (abs(shakeLeftTime - shakeRightTime) < detectionTime) {
                reset()
                return true
            } else {
                reset()
            }
        }
        return false
    }

    override fun reset() {
        shakeLeft = false
        shakeRight = false
        shakeLeftTime = 0L
        shakeRightTime = 0L
    }


    companion object {
        private const val TAG = "ShakeDetectionTask"
    }
}