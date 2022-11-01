package com.example.liveness.core

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object FaceUtils {

    private const val FACE_THRESHOLD_X = 20f
    private const val FACE_THRESHOLD_Y = 12f
    private const val FACE_THRESHOLD_Z = 8f

    fun isFaceCamera(face: Face): Boolean {
        return face.headEulerAngleZ < FACE_THRESHOLD_Z && face.headEulerAngleZ > -FACE_THRESHOLD_Z
                && face.headEulerAngleY < FACE_THRESHOLD_Y && face.headEulerAngleY > -FACE_THRESHOLD_Y
                && face.headEulerAngleX < FACE_THRESHOLD_X && face.headEulerAngleX > -FACE_THRESHOLD_X
    }

    fun isMouthOpened(face: Face): Boolean {
        val left = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return false
        val right = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return false
        val bottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        // Square of lengths be a2, b2, c2
        val a2 = lengthSquare(right, bottom)
        val b2 = lengthSquare(left, bottom)
        val c2 = lengthSquare(left, right)

        // length of sides be a, b, c
        val a = sqrt(a2)
        val b = sqrt(b2)

        // From Cosine law
        val gamma = acos((a2 + b2 - c2) / (2 * a * b))

        // Converting to degrees
        val gammaDeg = gamma * 180 / Math.PI
        return gammaDeg < 115f
    }

    private fun lengthSquare(a: PointF, b: PointF): Float {
        val x = a.x - b.x
        val y = a.y - b.y
        return x * x + y * y
    }
}