package com.aria.danesh.faceverification

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aria.danesh.faceverification.ui.theme.FaceVerificationTheme
import com.aria.danesh.faceverificationlib.view.FaceVerificationComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceVerificationTheme {

                val bitmapSaver = rememberBitmapSaver()
                val context = LocalContext.current
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }

                FaceVerificationComponent(
                    onFaceDetected = {
                        bitmap = null
                    },
                    onFacesDetected = {
                        bitmap = null
                    },
                    noFaceDetected = {
                        bitmap = null
                    },
                    onError = {
                        bitmap = null
                    },

                    )
                bitmap?.let {
                    val filename = "my_image_${System.currentTimeMillis()}.jpg"
                    val uri = bitmapSaver(it, Bitmap.CompressFormat.JPEG, 90, filename)
                    if (uri != null) {
                        // Handle successful save (e.g., show a Toast)
                        Log.d("BitmapSaver", "Image saved at: $uri")
                        // You might want to trigger a UI update or show a message here
                    } else {
                        // Handle save failure
                        Log.e("BitmapSaver", "Failed to save image")
                        // Show an error message to the user
                    }
                }
            }
        }
    }
}

@Composable
fun rememberBitmapSaver(): (Bitmap, Bitmap.CompressFormat, Int, String) -> Uri? {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return { bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, filename: String ->
        var savedUri: Uri? = null
        coroutineScope.launch(Dispatchers.IO) {
            savedUri = saveBitmapInternal(context, bitmap, format, quality, filename)
        }
        savedUri // This will likely be null immediately as saving happens in a coroutine
    }
}

private fun saveBitmapInternal(
    context: Context,
    bitmap: Bitmap,
    format: Bitmap.CompressFormat,
    quality: Int,
    filename: String
): Uri? {
    var uri: Uri? = null
    try {
        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/${format.toString().lowercase()}")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/YourAppName"
                ) // Optional subfolder
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )?.let {
                context.contentResolver.openOutputStream(it)
            }
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "YourAppName"
            ) // Ensure directory exists
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, filename)
            FileOutputStream(file)
        }

        outputStream?.use {
            bitmap.compress(format, quality, it)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, it.toString())
                values.put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/${format.toString().lowercase()}"
                )
                uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            } else {
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media.DATA),
                    MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    arrayOf(filename),
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val path =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                        uri = Uri.fromFile(File(path))
                    }
                }
            }
        }
    } catch (e: IOException) {
        Log.e("BitmapSaver", "Error saving bitmap: ${e.message}")
    }
    return uri
}

