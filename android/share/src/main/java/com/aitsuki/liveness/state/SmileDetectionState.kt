package com.aitsuki.liveness.state

import com.aitsuki.liveness.DetectionUtils
import com.google.mlkit.vision.face.Face

class SmileDetectionState : ConsecutiveFramesDetectionState() {

    override fun isPass(face: Face): Boolean {
        return DetectionUtils.isFrontFace(face) && isSmiling(face)
    }

    private fun isSmiling(face: Face): Boolean {
        return face.smilingProbability?.let { it > 0.6f } ?: false
    }

}