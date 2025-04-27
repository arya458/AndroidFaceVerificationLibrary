package com.aria.danesh.faceverificationlib.callback

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face

/**
 * Callback interface for receiving face detection results and state updates.
 */
interface FaceDetectionCallback {

    /**
     * Called specifically when multiple faces (more than one) are detected in the
     * current frame. This provides a separate callback for handling scenarios
     * where multiple faces are present.
     *
     * @param faces A list of nullable [com.google.mlkit.vision.face.Face] objects, representing the multiple
     * faces detected.
     */
    fun onManyFacesDetected(faces: List<Face?>,imageProxy: ImageProxy)

    /**
     * Called when exactly one face is detected in the current frame. This callback
     * is specifically for situations where you expect or are interested in a single
     * detected face.
     *
     * @param face A non-nullable [Face] object representing the single detected face.
     * Note: This parameter is non-nullable, implying that this callback
     * is only invoked when a face is successfully detected.
     */
    fun onOneFaceDetected(face: Face,imageProxy: ImageProxy)

    /**
     * Called when no face is detected in the current frame.
     *
     * @param face This parameter seems counter-intuitive as it's provided when *no*
     * face is detected. It's likely a mistake or intended for a specific
     * use case that isn't immediately clear. If no face is detected,
     * you would typically expect an empty list or no `Face` object at all.
     * Consider reviewing the logic where this callback is invoked.
     */
    fun onNoFaceDetected(imageProxy: ImageProxy)

    /**
     * Called when an error occurs during the face detection process.
     *
     * @param exception The [Exception] that occurred. Consumers of the library should handle
     * this to inform the user or log the error appropriately.
     */
    fun onFaceDetectionError(exception: Exception)

    /**
     * Called when the state of the face detection process changes. This can be useful
     * for updating the UI or managing the lifecycle of the detection.
     *
     * @param state The current [com.aria.danesh.faceverificationlib.state.FaceState] of the detection process.
     */
}