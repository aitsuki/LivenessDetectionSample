package com.example.liveness.core.state

import com.example.liveness.core.DetectionUtils
import com.google.mlkit.vision.face.Face

class FrontFaceDetectionState : ConsecutiveFramesDetectionState() {
    override fun isPass(face: Face): Boolean {
        return DetectionUtils.isFrontFace(face)
    }

}