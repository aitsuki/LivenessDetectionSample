package com.aitsuki.liveness.state

import com.aitsuki.liveness.DetectionContext
import com.google.mlkit.vision.face.Face

interface DetectionState {
    fun handleState(context: DetectionContext, face: Face)
}