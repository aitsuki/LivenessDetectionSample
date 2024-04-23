package com.example.liveness.core

import com.example.liveness.core.state.DetectionState
import com.google.mlkit.vision.face.Face

class DetectionContext(
    states: List<DetectionState>,
    private val stateChangeCallback: StateChangeCallback,
    private val onCompleteListener: OnCompleteListener
) {
    private val states: ArrayList<DetectionState>

    init {
        this.states = ArrayList(states.size + 1)
        this.states.addAll(states)
        // 哨兵
        this.states.add(object : DetectionState {
            override fun handleState(context: DetectionContext, face: Face) {}
        })
    }

    private var stateIndex = 0
    private var stateTransitioning = false

    fun getCurrentState(): DetectionState {
        return states[stateIndex]
    }

    fun handle(face: Face) {
        if (stateTransitioning) return
        getCurrentState().handleState(this, face)
    }

    /**
     * 每次转移状态时需要对当前状态进行拍照
     */
    internal fun nextState() {
        if (stateTransitioning) {
            return
        }
        stateTransitioning = true
        stateChangeCallback.onNext(
            retry = {
                stateTransitioning = false
            }, next = {
                // 二次确认状态转移，回调执行过程中状态可能已经转移，比如外部调用了resetState
                if (stateTransitioning) {
                    stateTransitioning = false
                    stateIndex++
                    if (stateIndex == states.lastIndex) {
                        onCompleteListener.onComplete()
                    }
                }
            })
    }

    fun resetState() {
        stateTransitioning = false
        stateIndex = 0
    }


    interface StateChangeCallback {
        fun onNext(next: () -> Unit, retry: () -> Unit)
    }

    interface OnCompleteListener {
        fun onComplete()
    }
}
