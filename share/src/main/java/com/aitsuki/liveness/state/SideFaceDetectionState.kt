package com.aitsuki.liveness.state

import com.aitsuki.liveness.DetectionContext
import com.aitsuki.liveness.DetectionUtils
import com.google.mlkit.vision.face.Face

class SideFaceDetectionState : DetectionState {

    override fun handleState(context: DetectionContext, face: Face) {
        if (DetectionUtils.isSideFace(face)) {
            context.nextState()
        }
    }
}