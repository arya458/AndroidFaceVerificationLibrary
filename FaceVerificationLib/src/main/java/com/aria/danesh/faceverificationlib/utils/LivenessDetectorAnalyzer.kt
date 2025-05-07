package com.aria.danesh.faceverificationlib.utils

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class LivenessDetectorAnalyzer(
    private val onLivenessResult: (LivenessState) -> Unit,
    private val requiredBlinks: Int = 1,
    private val motionThreshold: Float = 5f, // Threshold for landmark movement
    private val motionDetectionWindowMs: Long = 500L
) {

    private var blinkCount = 0
    private var isLeftEyeClosed = false
    private var isRightEyeClosed = false
    private var lastBlinkTime = 0L
    private val blinkDebounceThreshold = 200L

    private var previousFace: Face? = null
    private var lastMotionDetectionTime = 0L

    private val _livenessState = MutableStateFlow<LivenessState>(LivenessState.Initial)

    init {
        onLivenessResult(LivenessState.Initial)
    }

    internal fun processFace(currentFace: Face,ip : ImageProxy) {
        detectBlink(currentFace)
        detectMotion(currentFace)

        if (blinkCount >= requiredBlinks) {
            _livenessState.value = LivenessState.Success(imageProxy = ip)
            onLivenessResult(LivenessState.Success(imageProxy = ip))
            // Optionally stop analysis here if liveness is confirmed
        } else {
            _livenessState.value = LivenessState.Processing("Detected $blinkCount/$requiredBlinks blinks.")
            onLivenessResult(LivenessState.Processing("Detected $blinkCount/$requiredBlinks blinks."))
        }
        previousFace = currentFace
    }

    private fun detectBlink(currentFace: Face) {
        val leftEyeOpenProbability = currentFace.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProbability = currentFace.rightEyeOpenProbability ?: 1.0f

        val currentLeftClosed = leftEyeOpenProbability < 0.2f
        val currentRightClosed = rightEyeOpenProbability < 0.2f
        val currentTime = System.currentTimeMillis()

        if ((!isLeftEyeClosed && !isRightEyeClosed && currentLeftClosed && currentRightClosed) &&
            (currentTime - lastBlinkTime > blinkDebounceThreshold)) {
            blinkCount++
            lastBlinkTime = currentTime
        }

        isLeftEyeClosed = currentLeftClosed
        isRightEyeClosed = currentRightClosed
    }

    private fun detectMotion(currentFace: Face) {
        previousFace?.let { prevFace ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMotionDetectionTime > motionDetectionWindowMs) {
                val noseBaseCurrent = currentFace.getLandmark(FaceLandmark.NOSE_BASE)
                val noseBasePrevious = prevFace.getLandmark(FaceLandmark.NOSE_BASE)

                if (noseBaseCurrent != null && noseBasePrevious != null) {
                    val deltaX = abs(noseBaseCurrent.position.x - noseBasePrevious.position.x)
                    val deltaY = abs(noseBaseCurrent.position.y - noseBasePrevious.position.y)

                    if (deltaX > motionThreshold || deltaY > motionThreshold) {
                        // Consider motion as a sign of liveness (can be combined with blink)
                        if (_livenessState.value is LivenessState.Processing) {
                            _livenessState.value = LivenessState.Processing("Detected $blinkCount/$requiredBlinks blinks and motion.")
                            onLivenessResult(LivenessState.Processing("Detected $blinkCount/$requiredBlinks blinks and motion."))
                        } else if (_livenessState.value is LivenessState.Initial) {
                            _livenessState.value = LivenessState.Processing("Detected initial motion.")
                            onLivenessResult(LivenessState.Processing("Detected initial motion."))
                        }
                        lastMotionDetectionTime = currentTime
                    }
                }
            }
        }
    }

    internal fun resetLiveness() {
        blinkCount = 0
        isLeftEyeClosed = false
        isRightEyeClosed = false
        lastBlinkTime = 0L
        previousFace = null
        lastMotionDetectionTime = 0L
    }
}

sealed class LivenessState {
    object Initial : LivenessState()
    data class Processing(val message: String) : LivenessState()
    data class Success(val imageProxy:ImageProxy) : LivenessState()
    data class Failed(val error: String) : LivenessState()
}