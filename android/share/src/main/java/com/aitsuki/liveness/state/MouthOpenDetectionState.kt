package com.aitsuki.liveness.state

import com.aitsuki.liveness.DetectionUtils
import com.google.mlkit.vision.face.Face

class MouthOpenDetectionState : ConsecutiveFramesDetectionState() {
    override fun isPass(face: Face): Boolean {
        return DetectionUtils.isMouthOpened(face)
    }
}