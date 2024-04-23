package com.example.liveness.core

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object DetectionUtils {

    private const val YAW_THRESHOLD = 12f
    private const val PITCH_THRESHOLD = 8f
    private const val ROLL_THRESHOLD = 8f
    private const val SIDE_FACE_YAW_THRESHOLD = 20f

    fun isFrontFace(face: Face): Boolean {
        val yaw = face.headEulerAngleY // 左右摇头角度
        val pitch = face.headEulerAngleX // 上下点头角度
        val roll = face.headEulerAngleZ // 旋转角度
        return yaw < YAW_THRESHOLD && yaw > -YAW_THRESHOLD
                && pitch < PITCH_THRESHOLD && pitch > -PITCH_THRESHOLD
                && roll < ROLL_THRESHOLD && roll > -ROLL_THRESHOLD
    }

    fun isSideFace(face: Face): Boolean {
        val yaw = face.headEulerAngleY // 左右摇头角度
        val pitch = face.headEulerAngleX // 上下点头角度
        val roll = face.headEulerAngleZ // 旋转角度
        return (yaw > SIDE_FACE_YAW_THRESHOLD || yaw < -SIDE_FACE_YAW_THRESHOLD)
                && pitch < PITCH_THRESHOLD && pitch > -PITCH_THRESHOLD
                && roll < ROLL_THRESHOLD && roll > -ROLL_THRESHOLD
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

    fun isWholeFace(face: Face, imageWidth: Int, imageHeight: Int): Boolean {
        val boundingBox = face.boundingBox
        val contours = face.getContour(FaceContour.FACE)
        val top = boundingBox.top
        val bottom = boundingBox.bottom
        val left = contours?.points?.getOrNull(27)?.x?.toInt() ?: boundingBox.left
        val right = contours?.points?.getOrNull(9)?.x?.toInt() ?: boundingBox.right
        return top > 0 && bottom < imageHeight && left > 0 && right < imageWidth
    }
}