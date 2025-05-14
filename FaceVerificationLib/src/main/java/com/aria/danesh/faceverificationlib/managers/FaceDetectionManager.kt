package com.aria.danesh.faceverificationlib.managers

import android.R.attr.height
import android.R.attr.width
import android.content.Context
import android.graphics.PointF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableOpenTarget
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aria.danesh.faceverificationlib.callback.FaceDetectionCallback
import com.aria.danesh.faceverificationlib.view.compose.UniversalData.isFrontCamera
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

fun cameraSelector(lensFacing: Int = CameraSelector.LENS_FACING_BACK): CameraSelector {
    isFrontCamera = lensFacing==CameraSelector.LENS_FACING_FRONT
    return CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
}


@ExperimentalGetImage
class FaceDetectionManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val callback: FaceDetectionCallback,
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(context),
    private val detector: FaceDetector = faceDetector(),
    private val cameraSelector: CameraSelector =cameraSelector(),
    private val executor: ExecutorService = Executors.newFixedThreadPool(10)
) {


    init {
        startCamera()
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                detector.process(image)
                    .addOnSuccessListener { faces ->

                        when{
                            faces.isEmpty()->{
                                callback.onNoFaceDetected(imageProxy)
                            }

                            faces.size>1 ->{
                                callback.onManyFacesDetected(faces.toList(),imageProxy)
                            }

                            else->{
                                callback.onOneFaceDetected(faces[0],imageProxy)
                            }
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        callback.onFaceDetectionError(e)
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        preview.surfaceProvider = previewView.surfaceProvider

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            callback.onFaceDetectionError(e)
        }
    }

    fun stopCamera() {
        cameraProviderFuture.get()?.unbindAll()
        executor.shutdown()
    }

    @Composable
    fun OverLayoutView(){

    }
}