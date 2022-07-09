package com.aitsuki.liveness

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class LivenessDetectionFlow(private val tasks: List<Task>, private val listener: Listener?) {

    private var taskIndex = 0
    private var faceId: Int = -1

    init {
        if (tasks.isEmpty()) throw IllegalArgumentException("Flow at least one task.")
    }

    fun detect(face: Face): Status {
        if (faceId == -1) {
            faceId = face.trackingId ?: return Status.Failed
            listener?.onTaskStart(tasks[taskIndex])
        }
        if (face.trackingId != faceId) return Status.Failed
        if (taskIndex >= tasks.size) return Status.Success
        if (tasks[taskIndex].detectFrame(face)) {
            listener?.onTaskFinish(tasks[taskIndex])
            taskIndex++
            if (taskIndex < tasks.size) {
                listener?.onTaskStart(tasks[taskIndex])
            }
        }
        return Status.InProgress
    }

    interface Listener {
        fun onTaskStart(task: Task)
        fun onTaskFinish(task: Task)
    }

    enum class Status {
        InProgress, Success, Failed
    }

    interface Task {
        /**
         * @return return true if the task is finished, false is need continue.
         */
        fun detectFrame(face: Face): Boolean
    }

    class FacingCameraTask : Task {

        private var time = 0L

        // Keep 2 seconds facing the camera.
        override fun detectFrame(face: Face): Boolean {
            Log.d(
                TAG, "FacingCameraTask: headEulerAngleX: ${face.headEulerAngleX}, " +
                        "headEulerAngleY: ${face.headEulerAngleY}, " +
                        "headEulerAngleZ: ${face.headEulerAngleZ}"
            )
            if (time == 0L) {
                time = System.currentTimeMillis()
            }
            val isFacingCamera = face.headEulerAngleZ < 10f && face.headEulerAngleZ > -10f
                    && face.headEulerAngleY < 10f && face.headEulerAngleY > -10f
                    && face.headEulerAngleX < 10f && face.headEulerAngleX > -10f

            if (isFacingCamera) {
                if (System.currentTimeMillis() - time > 2000) {
                    return true
                }
            } else {
                time = System.currentTimeMillis()
            }
            return false
        }
    }

    class ShakeTask : Task {


        private var shakeLeft = false
        private var shakeRight = false
        private var shakeLeftTime = 0L
        private var shakeRightTime = 0L

        // Shake left and right in 3 seconds.
        override fun detectFrame(face: Face): Boolean {
            Log.d(TAG, "headEulerAngleY: ${face.headEulerAngleY}")

            val yaw = face.headEulerAngleY
            if (yaw > 25f) {
                shakeLeft = true
                shakeLeftTime = System.currentTimeMillis()
            } else if (yaw < -25f) {
                shakeRight = true
                shakeRightTime = System.currentTimeMillis()
            }
            if (shakeLeft && shakeRight) {
                if (abs(shakeLeftTime - shakeRightTime) < 3000) {
                    return true
                } else {
                    shakeLeft = false
                    shakeRight = false
                    shakeLeftTime = 0L
                    shakeRightTime = 0L
                }
            }
            return false
        }
    }

    class OpenMouthTask : Task {

        private var time: Long = 0L

        // https://stackoverflow.com/questions/42107466/android-mobile-vision-api-detect-mouth-is-open
        // Keep 2 seconds mouth is open.
        override fun detectFrame(face: Face): Boolean {
            if (time == 0L) {
                time = System.currentTimeMillis()
            }
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
            val c = sqrt(c2)

            // From Cosine law
            val alpha = acos((b2 + c2 - a2) / (2 * b * c))
            val beta = acos((a2 + c2 - b2) / (2 * a * c))
            val gamma = acos((a2 + b2 - c2) / (2 * a * b))

            // Converting to degrees
            val alphaDeg = alpha * 180 / Math.PI
            val betaDeg = beta * 180 / Math.PI
            val gammaDeg = gamma * 180 / Math.PI

            Log.d(
                "LivenessDetection",
                "OpenMouthTask: alpha: $alphaDeg, beta: $betaDeg, gamma: $gammaDeg"
            )

            if (gammaDeg < 120f) {
                if (System.currentTimeMillis() - time > 2000) {
                    return true
                }
            } else {
                time = System.currentTimeMillis()
            }
            return false
        }

        companion object {

            private fun lengthSquare(a: PointF, b: PointF): Float {
                val x = a.x - b.x
                val y = a.y - b.y
                return x * x + y * y
            }
        }
    }
}