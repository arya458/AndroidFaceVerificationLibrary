package com.aria.danesh.faceverificationlib.state

import androidx.camera.core.ImageProxy

sealed class LivelinessState {
    object Initial : LivelinessState()
    data class Processing(val message: String) : LivelinessState()
    data class Success(val imageProxy: ImageProxy) : LivelinessState()
    data class Failed(val error: String) : LivelinessState()
}