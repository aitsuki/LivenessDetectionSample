package com.example.liveness.core

import com.google.mlkit.vision.face.Face
import java.util.*

class LivenessDetector(vararg tasks: DetectionTask) {

    companion object {
        private const val FACE_CACHE_SIZE = 5
        private const val NO_ERROR = -1
        const val ERROR_NO_FACE = 0
        const val ERROR_MULTI_FACES = 1
        const val ERROR_OUT_OF_DETECTION_RECT = 2
    }

    init {
        check(tasks.isNotEmpty()) { "no tasks" }
    }

    private val tasks = tasks.asList()
    private var taskIndex = 0
    private var lastTaskIndex = -1
    private var currentErrorState = NO_ERROR
    private val lastFaces: Deque<Face> = LinkedList()
    private var listener: Listener? = null

    fun process(faces: List<Face>?, detectionSize: Int) {
        val task = tasks.getOrNull(taskIndex) ?: return
        if (taskIndex != lastTaskIndex) {
            lastTaskIndex = taskIndex
            task.start()
            listener?.onTaskStarted(task)
        }

        val face = filter(task, faces, detectionSize) ?: return
        if (task.process(face)) {
            listener?.onTaskCompleted(task, taskIndex == tasks.lastIndex)
            taskIndex++
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private fun reset() {
        taskIndex = 0
        lastTaskIndex = -1
        lastFaces.clear()
    }

    private fun filter(task: DetectionTask, faces: List<Face>?, detectionSize: Int): Face? {
        if (faces != null && faces.size > 1) {
            changeErrorState(task, ERROR_MULTI_FACES)
            reset()
            return null
        }

        if (faces.isNullOrEmpty() && lastFaces.isEmpty()) {
            changeErrorState(task, ERROR_NO_FACE)
            reset()
            return null
        }

        val face = faces?.firstOrNull() ?: lastFaces.pollFirst()
        if (!DetectionUtils.isFaceInDetectionRect(face, detectionSize)) {
            changeErrorState(task, ERROR_OUT_OF_DETECTION_RECT)
            reset()
            return null
        }

        if (!faces.isNullOrEmpty()) {
            // cache current face
            lastFaces.offerFirst(face)
            if (lastFaces.size > FACE_CACHE_SIZE) {
                lastFaces.pollLast()
            }
        }
        changeErrorState(task, NO_ERROR)
        return face
    }

    private fun changeErrorState(task: DetectionTask, newErrorState: Int) {
        if (newErrorState != currentErrorState) {
            currentErrorState = newErrorState
            if (currentErrorState != NO_ERROR) {
                listener?.onTaskFailed(task, currentErrorState)
            }
        }
    }

    interface Listener {

        fun onTaskStarted(task: DetectionTask)

        fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean)

        fun onTaskFailed(task: DetectionTask, code: Int)
    }
}