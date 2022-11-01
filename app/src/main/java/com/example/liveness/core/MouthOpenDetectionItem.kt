package com.example.liveness.core

import com.google.mlkit.vision.face.Face

class MouthOpenDetectionItem : DetectionItem {

    override fun detect(face: Face): Boolean {
        return FaceUtils.isFaceCamera(face) && FaceUtils.isMouthOpened(face)
    }
}