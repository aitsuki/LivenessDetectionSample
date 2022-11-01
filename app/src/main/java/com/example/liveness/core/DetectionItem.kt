package com.example.liveness.core

import com.google.mlkit.vision.face.Face

interface DetectionItem {
    fun prepare() {}
    fun detect(face: Face): Boolean
}