package com.aria.danesh.faceverificationlib.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aria.danesh.faceverificationlib.state.LivelinessState
import com.aria.danesh.faceverificationlib.utils.LivelinessDetectorAnalyzer


//@Composable
//fun rememberLivelinessDetectorAnalyzer(result :  (LivelinessState) -> Unit): LivelinessDetectorAnalyzer {
//    return remember {
//        LivelinessDetectorAnalyzer(onLivelinessResult = { result(it) })
//    }
//}

//@Composable
//internal fun CameraView(livelinessDetector : LivelinessDetectorAnalyzer = rememberLivelinessDetectorAnalyzer {  }) {
//
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//    val executor = remember { ContextCompat.getMainExecutor(context) }
//    val imageAnalysis = remember {
//        ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//            .also {
//                it.setAnalyzer(executor, livelinessDetector) // Set the LivenessDetectorAnalyzer
//            }
//    }
//
//    val cameraView = remember {
//        PreviewView(context).apply {
//            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
//        }
//    }
//
//    // Use LaunchedEffect to handle camera setup
//    androidx.compose.runtime.LaunchedEffect(lifecycleOwner) {  // Use lifecycleOwner
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder().build().also {
//                it.surfaceProvider = cameraView.surfaceProvider
//            }
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    lifecycleOwner,
//                    CameraSelector.DEFAULT_BACK_CAMERA,
//                    preview,
//                    imageAnalysis, // Bind the imageAnalysis
////                    faceVerificationViewModel().imageCapture
//                )
//            } catch (e: Exception) {
//                Log.e("CameraView", "Camera binding failed", e) // Log the error
//                // Handle the error appropriately, e.g., show a message to the user
//            }
//        }, executor)
//    }
//
//    AndroidView(
//        factory = { cameraView },
//        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
//    )
//}