package com.aria.danesh.faceverificationlib.state

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face

/**
 * Represents the UI state for the face verification screen.
 */
sealed class FaceUiState(val face: Face?, val imageProxy: ImageProxy?, val message: String) {
    object Initial : FaceUiState(null, null, "Initializing...")
    class Scanning(face: Face, imageProxy: ImageProxy, message: String) : FaceUiState(face, imageProxy, message)
    class Success(face: Face, imageProxy: ImageProxy, message: String) : FaceUiState(face, imageProxy, message)
    class Error(face: Face?, imageProxy: ImageProxy?, message: String) : FaceUiState(face, imageProxy, message)
}
