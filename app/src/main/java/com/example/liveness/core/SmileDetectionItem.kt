package com.example.liveness.core

import com.google.mlkit.vision.face.Face

class SmileDetectionItem : DetectionItem {

    override fun detect(face: Face): Boolean {
        val isSmile = (face.smilingProbability ?: 0f) > 0.7f
        return isSmile && FaceUtils.isFaceCamera(face)
    }
}