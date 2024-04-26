package com.aitsuki.liveness.state

import com.aitsuki.liveness.DetectionContext
import com.google.mlkit.vision.face.Face

abstract class ConsecutiveFramesDetectionState : DetectionState {

    private var currentFrame = 0

    open val totalFrames = 20
    abstract fun isPass(face: Face): Boolean

    override fun handleState(context: DetectionContext, face: Face) {
        if (isPass(face)) {
            currentFrame++
            if (currentFrame > totalFrames) {
                currentFrame = 0
                context.nextState()
            }
        } else {
            currentFrame = 0
        }
    }
}