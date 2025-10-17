package com.aria.danesh.faceverificationlib.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aria.danesh.faceverificationlib.R
import com.aria.danesh.faceverificationlib.callback.FaceDetectionCallback
import com.aria.danesh.faceverificationlib.managers.FaceDetectionManager
import com.aria.danesh.faceverificationlib.state.FaceDetectionState
import com.aria.danesh.faceverificationlib.state.FaceUiState
import com.aria.danesh.faceverificationlib.view.compose.FaceOverlay
import com.aria.danesh.faceverificationlib.view.compose.FaceStatusIndicator

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceVerificationComponent(
    onSmileDetected: (ImageProxy) -> Unit,
    config: FaceVerificationConfig = defaultFaceVerificationConfig()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // State for UI
    var uiState by remember { mutableStateOf<FaceUiState>(FaceUiState.Initial) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (cameraPermissionGranted) {
        val faceDetectionCallback = remember {
            object : FaceDetectionCallback {
                override fun onFaceDetectionStateChanged(state: FaceDetectionState) {
                    var imageProxyToClose: ImageProxy? = null
                    uiState = when (state) {
                        is FaceDetectionState.OneFace -> {
                            imageProxyToClose = state.imageProxy
                            val isSmiling = (state.face.smilingProbability ?: 0f) > config.smileThreshold
                            if (isSmiling) {
                                onSmileDetected(state.imageProxy)
                                FaceUiState.Success(state.face, state.imageProxy, "Smile Detected!")
                            } else {
                                FaceUiState.Scanning(state.face, state.imageProxy, "Please Smile")
                            }
                        }
                        is FaceDetectionState.ManyFaces -> {
                            imageProxyToClose = state.imageProxy
                            FaceUiState.Error(null, state.imageProxy, "Multiple Faces Detected")
                        }
                        is FaceDetectionState.NoFace -> {
                            imageProxyToClose = state.imageProxy
                            FaceUiState.Error(null, state.imageProxy, "No Face Detected")
                        }
                        is FaceDetectionState.Error -> {
                            FaceUiState.Error(null, null, state.exception.localizedMessage ?: "Detection Error")
                        }
                    }

                    // Ensure ImageProxy is closed if not passed to the success callback
                    val isSuccessState = uiState is FaceUiState.Success
                    if (!isSuccessState) {
                        imageProxyToClose?.close()
                    }
                }
            }
        }

        val faceDetectionManager = remember(faceDetectionCallback) {
            FaceDetectionManager(context, lifecycleOwner, previewView, faceDetectionCallback)
        }

        DisposableEffect(Unit) {
            onDispose {
                faceDetectionManager.stopCamera()
            }
        }

        Box(Modifier.fillMaxSize()) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            val overlayColor = when (uiState) {
                is FaceUiState.Success -> config.successColor
                is FaceUiState.Scanning -> config.primaryColor
                else -> config.errorColor
            }

            FaceOverlay(
                detectedFace = uiState.face,
                frameDrawableId = R.drawable.frame,
                imageProxy = uiState.imageProxy,
                isFrontCamera = true,
                color = overlayColor
            )

            if (uiState !is FaceUiState.Initial) {
                // Determine icon based on state
                val iconRes = when (uiState) {
                    is FaceUiState.Success, is FaceUiState.Scanning -> R.drawable.smile
                    else -> R.drawable.bad
                }

                FaceStatusIndicator(
                    message = uiState.message,
                    iconRes = iconRes,
                    iconTint = overlayColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                )
            }
        }
    } else {
        // A placeholder for when permission is not granted
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to use this feature.")
        }
    }
}
