package com.example.liveness.core.state

import com.example.liveness.core.DetectionContext
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