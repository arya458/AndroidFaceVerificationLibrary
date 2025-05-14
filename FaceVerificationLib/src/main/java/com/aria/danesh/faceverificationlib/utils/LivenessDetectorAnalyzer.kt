package com.aria.danesh.faceverificationlib.utils

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aria.danesh.faceverificationlib.state.LivelinessState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class LivelinessDetectorAnalyzer(
    private val onLivelinessResult: (LivelinessState) -> Unit,
    private val requiredBlinks: Int = 1,
    private val motionThreshold: Float = 5f, // Threshold for landmark movement
    private val motionDetectionWindowMs: Long = 500L,
    private val eyeOpenThreshold: Float = 0.2f //make it public
) : ImageAnalysis.Analyzer
{

    private var blinkCount = 0
    private var isLeftEyeClosed = false
    private var isRightEyeClosed = false
    private var lastBlinkTime = 0L
    private val blinkDebounceThreshold = 200L

    private var previousFace: Face? = null
    private var lastMotionDetectionTime = 0L

    private val _livelinessState = MutableStateFlow<LivelinessState>(LivelinessState.Initial)
    val livelinessState: StateFlow<LivelinessState> = _livelinessState

    init {
        onLivelinessResult(LivelinessState.Initial)
    }

    fun faceDetector(
        performance: Int = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE,
        landmark: Int = FaceDetectorOptions.LANDMARK_MODE_ALL,
        classification: Int = FaceDetectorOptions.CLASSIFICATION_MODE_ALL,
        contour: Int = FaceDetectorOptions.CONTOUR_MODE_ALL,
        minFaceSize: Float = 0.05f
    ): FaceDetector {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(performance)
            .setLandmarkMode(landmark)
            .setClassificationMode(classification)
            .setContourMode(contour)
            .setMinFaceSize(minFaceSize)
            .enableTracking()
            .build()
        return FaceDetection.getClient(faceDetectorOptions)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = faceDetector()
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        if (faces.size==1) {
                            val face = faces.last()
                            if (face.boundingBox.width() in 100..290 &&
                                face.boundingBox.height() in 100..290 &&
                                face.boundingBox.top in 100..300 &&
                                face.boundingBox.left in 100..300 &&
                                face.boundingBox.bottom in 300..500 &&
                                face.boundingBox.right in 300..500
                            ) {
                                Log.d(
                                    "LivenessDetector",
                                    "Face detected: ${faces.size} faces"
                                ) //basic face detection log
                                processFace(face, imageProxy)
                            }else{
                                _livelinessState.value = LivelinessState.Failed("Face isn't in the center of the frame")
                                onLivelinessResult(LivelinessState.Failed("Face isn't in the center of the frame"))
                            }
                        }
                    } else {
                        resetLiveness()
                        _livelinessState.value = LivelinessState.Failed("No face detected")
                        onLivelinessResult(LivelinessState.Failed("No face detected"))
                        Log.d("LivenessDetector", "No face detected")  //log when no face
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LivenessDetector", "Face detection failed: ${e.message}")
                    resetLiveness()
                    _livelinessState.value = LivelinessState.Failed("Face detection failed: ${e.message}")
                    onLivelinessResult(LivelinessState.Failed("Face detection failed: ${e.message}"))
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
            Log.d("LivenessDetector", "ImageProxy image is null") //check if the image is null
        }
    }

    internal fun processFace(currentFace: Face, ip: ImageProxy) {

        detectBlink(currentFace)
        detectMotion(currentFace)
        if (blinkCount >= requiredBlinks) {
            _livelinessState.value = LivelinessState.Success(imageProxy = ip)
            onLivelinessResult(LivelinessState.Success(imageProxy = ip))
            Log.d("LivenessDetector", "Liveness Success. Blink Count: $blinkCount")
        } else {
            _livelinessState.value = LivelinessState.Processing("Detected $blinkCount/$requiredBlinks blinks.")
            onLivelinessResult(LivelinessState.Processing("Detected $blinkCount/$requiredBlinks blinks."))
        }
        previousFace = currentFace
    }

    private fun detectBlink(currentFace: Face) {
        val leftEyeOpenProbability = currentFace.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProbability = currentFace.rightEyeOpenProbability ?: 1.0f

        val currentLeftClosed = leftEyeOpenProbability < eyeOpenThreshold
        val currentRightClosed = rightEyeOpenProbability < eyeOpenThreshold
        val currentTime = System.currentTimeMillis()

        Log.d(
            "LivenessDetector",
            "Eye Probabilities: Left = $leftEyeOpenProbability, Right = $rightEyeOpenProbability, threshold = $eyeOpenThreshold"
        )  //added logs

        if ((!isLeftEyeClosed && !isRightEyeClosed && currentLeftClosed && currentRightClosed) &&
            (currentTime - lastBlinkTime > blinkDebounceThreshold)) {
            blinkCount++
            lastBlinkTime = currentTime
            Log.d("LivenessDetector", "Blink Detected! Count: $blinkCount") //log when blink is detected
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

                    Log.d(
                        "LivenessDetector",
                        "Motion: DeltaX = $deltaX, DeltaY = $deltaY"
                    ) //log motion values

                    if (deltaX > motionThreshold || deltaY > motionThreshold) {
                        // Consider motion as a sign of liveness (can be combined with blink)
                        if (_livelinessState.value is LivelinessState.Processing) {
                            _livelinessState.value =
                                LivelinessState.Processing("Detected $blinkCount/$requiredBlinks blinks and motion.")
                            onLivelinessResult(
                                LivelinessState.Processing("Detected $blinkCount/$requiredBlinks blinks and motion.")
                            )
                        } else if (_livelinessState.value is LivelinessState.Initial) {
                            _livelinessState.value = LivelinessState.Processing("Detected initial motion.")
                            onLivelinessResult(LivelinessState.Processing("Detected initial motion."))
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
        _livelinessState.value = LivelinessState.Initial //reset
    }
}



