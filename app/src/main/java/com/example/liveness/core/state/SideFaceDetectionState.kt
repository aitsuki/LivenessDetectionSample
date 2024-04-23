package com.example.liveness.core.state

import com.example.liveness.core.DetectionContext
import com.example.liveness.core.DetectionUtils
import com.google.mlkit.vision.face.Face

class SideFaceDetectionState : DetectionState {

    override fun handleState(context: DetectionContext, face: Face) {
        if (DetectionUtils.isSideFace(face)) {
            context.nextState()
        }
    }
}