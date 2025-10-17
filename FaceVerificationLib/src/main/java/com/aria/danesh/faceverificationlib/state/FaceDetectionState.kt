package com.aria.danesh.faceverificationlib.state

import androidx.camera.core.ImageProxy

/**
 * Represents the state of the face detection process.
 */

sealed class FaceDetectionState {
    /** State when exactly one face is detected. */
    data class OneFace(val face: com.google.mlkit.vision.face.Face, val imageProxy: ImageProxy) : FaceDetectionState()

    /** State when multiple faces are detected. */
    data class ManyFaces(val faces: List<com.google.mlkit.vision.face.Face>, val imageProxy: ImageProxy) : FaceDetectionState()

    /** State when no face is detected. */
    data class NoFace(val imageProxy: ImageProxy) : FaceDetectionState()

    /** State when an error occurs during detection. */
    data class Error(val exception: Exception) : FaceDetectionState()
}
