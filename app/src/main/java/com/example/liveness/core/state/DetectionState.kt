package com.example.liveness.core.state

import com.example.liveness.core.DetectionContext
import com.google.mlkit.vision.face.Face

interface DetectionState {
    fun handleState(context: DetectionContext, face: Face)
}