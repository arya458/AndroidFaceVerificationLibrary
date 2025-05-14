package com.aria.danesh.faceverificationlib.view

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aria.danesh.faceverificationlib.R
import com.aria.danesh.faceverificationlib.callback.FaceDetectionCallback
import com.aria.danesh.faceverificationlib.managers.FaceDetectionManager
import com.aria.danesh.faceverificationlib.managers.cameraSelector
import com.aria.danesh.faceverificationlib.managers.faceDetector
import com.aria.danesh.faceverificationlib.state.LivelinessState
import com.aria.danesh.faceverificationlib.utils.LivelinessDetectorAnalyzer
import com.aria.danesh.faceverificationlib.view.compose.FaceOverlay
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions

@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceVerificationComponent(
    onFaceDetected: (LivelinessState) -> Unit,
    onFacesDetected: () -> Unit,
    noFaceDetected: () -> Unit,
    onError: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var detectedFace by remember { mutableStateOf<Face?>(null) }
    var faceDetectionError by remember { mutableStateOf<String>("") }
    var imageProxy by remember { mutableStateOf<ImageProxy?>(null) }
    var faceLiveliness by remember { mutableStateOf<LivelinessState>(LivelinessState.Initial) }

    val livelinessDetector = remember {
        LivelinessDetectorAnalyzer(
            onLivelinessResult = { faceLiveliness = it },
            requiredBlinks = 2, // Require two blinks
            motionThreshold = 8f, // Increase motion sensitivity
            motionDetectionWindowMs = 750L // Adjust motion window
        )
    }


    val pColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    var color by remember { mutableStateOf<Color>(pColor) }
    var icon by remember { mutableIntStateOf(0) }


    val faceDetectionCallback = remember {
        object : FaceDetectionCallback {
            override fun onOneFaceDetected(face: Face, ip: ImageProxy) {
                faceDetectionError = when (faceLiveliness) {
                    is LivelinessState.Initial -> {
                        "Initial"
                    }

                    is LivelinessState.Success -> {
                        color = Color.Green
                        "Success"
                    }

                    is LivelinessState.Failed -> {
                        color = Color.Red
                        "Failed"
                    }

                    is LivelinessState.Processing -> {
                        color = Color.Blue
                        "Processing"
                    }
                }
                detectedFace = face
                icon = R.drawable.smile
                livelinessDetector.processFace(face, ip)
                onFaceDetected(faceLiveliness)
                imageProxy = ip
            }

            override fun onManyFacesDetected(faces: List<Face?>, ip: ImageProxy) {
                faceDetectionError = "Many Faces Detected"
                icon = R.drawable.bad
                detectedFace = null
                color = errorColor
                imageProxy = ip
                livelinessDetector.resetLiveness()
                onFacesDetected()
            }

            override fun onNoFaceDetected(ip: ImageProxy) {
                faceDetectionError = "No Face Detected"
                icon = R.drawable.bad
                detectedFace = null
                color = errorColor
                imageProxy = ip
                livelinessDetector.resetLiveness()
                noFaceDetected()
            }


            override fun onFaceDetectionError(exception: Exception) {
                faceDetectionError = exception.localizedMessage ?: "Face detection error"
                icon = R.drawable.bad
                detectedFace = null
                color = errorColor
                livelinessDetector.resetLiveness()
                onError()
            }
        }
    }
    val faceDetectionManager = remember(lifecycleOwner, previewView, faceDetectionCallback) {
        FaceDetectionManager(context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            detector = faceDetector(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE,FaceDetectorOptions.LANDMARK_MODE_ALL,FaceDetectorOptions.CLASSIFICATION_MODE_ALL,FaceDetectorOptions.CONTOUR_MODE_ALL),
            cameraSelector = cameraSelector(CameraSelector.LENS_FACING_FRONT),
            callback = faceDetectionCallback)
    }
    DisposableEffect(lifecycleOwner, faceDetectionManager) {
        onDispose {
            faceDetectionManager.stopCamera()
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        detectedFace?.let { face ->
            FaceOverlay(detectedFace=face,frameDrawableId= R.drawable.frame,imageProxy= imageProxy, color=color)
        }

        if (icon != 0)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentSize(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 5.dp,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier
                            .wrapContentSize()
                            .padding(20.dp),
                        Arrangement.Start,
                        Alignment.CenterVertically
                    ) {

                        Image(
                            painter = painterResource(icon),
                            "",
                            Modifier.size(40.dp),
                            colorFilter = ColorFilter.tint(color)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            faceDetectionError,
                            style = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.wrapContentSize()
                        )

                    }
                }
            }

    }
}

