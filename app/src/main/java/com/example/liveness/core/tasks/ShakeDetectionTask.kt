package com.example.liveness.core.tasks

import com.example.liveness.core.DetectionTask
import com.google.mlkit.vision.face.Face

class ShakeDetectionTask : DetectionTask {

    companion object {
        private const val SHAKE_THRESHOLD = 18f
    }

    private var hasShakeToLeft = false
    private var hasShakeToRight = false

    override fun start() {
        hasShakeToLeft = false
        hasShakeToRight = false
    }

    override fun process(face: Face): Boolean {
        val yaw = face.headEulerAngleY
        if (yaw > SHAKE_THRESHOLD && !hasShakeToLeft) {
            hasShakeToLeft = true
        } else if (yaw < -SHAKE_THRESHOLD && !hasShakeToRight) {
            hasShakeToRight = true
        }
        return hasShakeToLeft || hasShakeToRight
    }
}