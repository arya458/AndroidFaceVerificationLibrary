package com.aria.danesh.faceverification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.aria.danesh.faceverification.ui.theme.FaceVerificationTheme
import com.aria.danesh.faceverificationlib.view.FaceVerificationComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceVerificationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        FaceVerificationTestScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun FaceVerificationTestScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State to hold the URI of the saved image
    var savedImageUri by remember { mutableStateOf<Uri?>(null) }

    // If an image has been saved, display it.
    if (savedImageUri != null) {
        CapturedImageScreen(
            imageUri = savedImageUri!!,
            onRetake = { savedImageUri = null } // Resetting the URI will show the camera again
        )
    } else {
        // Otherwise, show the face verification camera view.
        FaceVerificationComponent(
            onSmileDetected = { imageProxy ->
                Log.d("MainActivity", "Smile detected! Processing image.")
                coroutineScope.launch {
                    // Convert the ImageProxy to a Bitmap on a background thread
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close() // IMPORTANT: Close the proxy after use.

                    if (bitmap != null) {
                        // Save the bitmap and get the URI
                        val uri = saveBitmap(context, bitmap, "smile_capture.jpg")
                        if (uri != null) {
                            Log.d("MainActivity", "Image saved successfully at: $uri")
                            savedImageUri = uri // Update state to show the captured image
                        } else {
                            Log.e("MainActivity", "Failed to save bitmap.")
                        }
                    } else {
                        Log.e("MainActivity", "Could not convert ImageProxy to Bitmap.")
                    }
                }
            }
        )
    }
}

@Composable
fun CapturedImageScreen(imageUri: Uri, onRetake: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Smile Captured!", style = MaterialTheme.typography.headlineMedium)
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "Captured Smile",
            modifier = Modifier
                .fillMaxSize(0.8f)
                .padding(vertical = 20.dp),
            contentScale = ContentScale.Crop
        )
        Button(onClick = onRetake) {
            Text("Take Another Picture")
        }
    }
}

// --- Image Saving and Conversion Utilities ---

/**
 * Converts an ImageProxy (typically in YUV_420_888 format) to a Bitmap, handling rotation and mirroring.
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    if (image.format != ImageFormat.YUV_420_888) {
        Log.e("ImageProxyToBitmap", "Unsupported image format: ${image.format}")
        return null
    }

    val yBuffer = image.planes[0].buffer // Y
    val uBuffer = image.planes[1].buffer // U
    val vBuffer = image.planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
    val imageBytes = out.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // A null check is essential here
    if (bitmap == null) {
        Log.e("ImageProxyToBitmap", "BitmapFactory failed to decode the byte array.")
        return null
    }

    // Rotate and flip the bitmap to match the device's orientation for the front camera
    val matrix = Matrix().apply {
        postRotate(image.imageInfo.rotationDegrees.toFloat())
        // Flip horizontally for front camera
        postScale(-1f, 1f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}


/**
 * Saves a bitmap to the device's public pictures directory.
 * This is a suspend function to ensure it's called from a coroutine.
 */
private suspend fun saveBitmap(context: Context, bitmap: Bitmap, filename: String): Uri? {
    return withContext(Dispatchers.IO) {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FaceVerificationApp")
        }

        val resolver = context.contentResolver
        var uri: Uri? = null
        var stream: OutputStream? = null
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) {
                throw java.io.IOException("Failed to create new MediaStore record.")
            }
            stream = resolver.openOutputStream(uri)
            if (stream == null) {
                throw java.io.IOException("Failed to get output stream.")
            }
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw java.io.IOException("Failed to save bitmap.")
            }
            uri
        } catch (e: Exception) {
            Log.e("BitmapSaver", "Error saving bitmap: ${e.message}")
            // If something goes wrong, clean up the MediaStore entry
            uri?.let { orphanUri ->
                resolver.delete(orphanUri, null, null)
            }
            null
        } finally {
            stream?.close()
        }
    }
}
