package com.aitsuki.liveness.tasks

import android.util.Log
import com.aitsuki.liveness.LivenessAnalyzer
import com.google.mlkit.vision.face.Face

class FacingDetectionTask(private val detectionTime: Long = 2000) : LivenessAnalyzer.DetectionTask {

    private var time = 0L

    override fun process(face: Face): Boolean {
        Log.d(
            TAG, "headEulerAngleX: ${face.headEulerAngleX}, " +
                    "headEulerAngleY: ${face.headEulerAngleY}, " +
                    "headEulerAngleZ: ${face.headEulerAngleZ}"
        )

        if (time == 0L) {
            time = System.currentTimeMillis()
        }
        val isFacingCamera = face.headEulerAngleZ < 10f && face.headEulerAngleZ > -10f
                && face.headEulerAngleY < 10f && face.headEulerAngleY > -10f
                && face.headEulerAngleX < 10f && face.headEulerAngleX > -10f

        if (isFacingCamera) {
            if (System.currentTimeMillis() - time > detectionTime) {
                reset()
                return true
            }
        } else {
            time = 0L
        }
        return false
    }

    override fun reset() {
        time = 0L
    }

    companion object {
        private const val TAG = "FacingDetectionTask"
    }
}