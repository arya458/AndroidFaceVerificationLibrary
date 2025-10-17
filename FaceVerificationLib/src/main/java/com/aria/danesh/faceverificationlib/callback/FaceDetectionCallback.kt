package com.aria.danesh.faceverificationlib.callback

import androidx.camera.core.ImageProxy
import com.aria.danesh.faceverificationlib.state.FaceDetectionState




/**
 * Callback interface for receiving face detection state updates.
 */
interface FaceDetectionCallback {

    /**
     * Called when the state of the face detection process changes.
     * This single callback handles all possible outcomes, simplifying implementation.
     *
     * @param state The current [FaceDetectionState] of the detection process.
     */
    fun onFaceDetectionStateChanged(state: FaceDetectionState)
}
