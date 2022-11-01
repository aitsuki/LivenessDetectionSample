package com.example.liveness.core

import com.google.mlkit.vision.face.Face

class ShakeDetectionItem : DetectionItem {

    companion object {
        private const val THRESHOLD = 18f
    }

    private var hasShakeToLeft = false
    private var hasShakeToRight = false

    override fun prepare() {
        hasShakeToLeft = false
        hasShakeToRight = false
    }

    override fun detect(face: Face): Boolean {
        val yaw = face.headEulerAngleY
        if (yaw > THRESHOLD && !hasShakeToLeft) {
            hasShakeToLeft = true
        } else if (yaw < -THRESHOLD && !hasShakeToRight) {
            hasShakeToRight = true
        }
        return hasShakeToLeft || hasShakeToRight
    }
}