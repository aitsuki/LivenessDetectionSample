package com.example.liveness.core

import com.google.mlkit.vision.face.Face

class FacingDetectionItem : DetectionItem {

    companion object {
        private const val FACING_CAMERA_KEEP_TIME = 1000L
    }

    private var startTime = 0L

    override fun prepare() {
        startTime = System.currentTimeMillis()
    }

    override fun detect(face: Face): Boolean {
        if (!FaceUtils.isFaceCamera(face)) {
            startTime = System.currentTimeMillis()
            return false
        }
        return System.currentTimeMillis() - startTime >= FACING_CAMERA_KEEP_TIME
    }
}