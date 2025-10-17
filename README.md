# 🪞 Android Face Verification Library

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/reference/android/os/Build.VERSION_CODES)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

A **modern**, **lightweight**, and **highly configurable** Jetpack Compose library for **real-time face detection and verification**.  
Built with **CameraX**, **ML Kit**, and **Compose**, this library guides users to capture high-quality facial images — for example, when they smile — with minimal effort.

![Library Demo](https://i.imgur.com/your-demo-image.gif)
> *(Replace with a real demo GIF of your library in action)*

---

## ✨ Features

- 🎯 **Real-Time Face Detection** — Powered by Google ML Kit.
- 😄 **Smile Detection** — Automatically captures when the user smiles.
- 🔄 **Lifecycle-Aware** — Seamlessly integrates with Compose and CameraX lifecycles.
- ⚡ **Simple Integration** — Add face verification with just a few lines of code.
- 🎨 **Customizable UI** — Easily tweak colors, thresholds, and states.
- 🧠 **Optimized & Reliable** — Properly manages `ImageProxy` to avoid memory leaks and stalls.

---

## ⚙️ Setup

### 1. Add Dependencies

In your app-level `build.gradle.kts`, include the required dependencies:

```kotlin
dependencies {
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Google ML Kit for Face Detection
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Coil (for loading captured images)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Include this library as a module
    implementation(project(":FaceVerificationLib"))
}
```

### 2. Add Permissions

Add the following to your `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.camera.any" />
<uses-permission android:name="android.permission.CAMERA" />

<!-- Optional: for saving images to storage -->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

> ✅ Runtime permissions for the camera are handled automatically by the library.

---

## 🕹️ Usage — *As Simple As a Smile!*

Using the library is straightforward. Just include `FaceVerificationComponent()` in your Composable and handle the `onSmileDetected` callback.

Here’s a full example:

```kotlin
package com.your.package

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.aria.danesh.faceverificationlib.utils.imageProxyToBitmap
import com.aria.danesh.faceverificationlib.view.FaceVerificationComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FaceVerificationTestScreen()
                }
            }
        }
    }
}

@Composable
fun FaceVerificationTestScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var savedImageUri by remember { mutableStateOf<Uri?>(null) }

    if (savedImageUri != null) {
        CapturedImageScreen(
            imageUri = savedImageUri!!,
            onRetake = { savedImageUri = null }
        )
    } else {
        FaceVerificationComponent(
            onSmileDetected = { imageProxy ->
                Log.d("MainActivity", "Smile detected! Processing image...")
                coroutineScope.launch {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    if (bitmap != null) {
                        val uri = saveBitmap(context, bitmap, "smile_capture.jpg")
                        savedImageUri = uri
                    } else {
                        Log.e("MainActivity", "Failed to convert ImageProxy to Bitmap.")
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
        Button(onClick = onRetake) { Text("Take Another Picture") }
    }
}

private suspend fun saveBitmap(context: Context, bitmap: Bitmap, filename: String): Uri? {
    return withContext(Dispatchers.IO) {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/YourAppName")
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw java.io.IOException("Failed to create MediaStore record.")

            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw java.io.IOException("Failed to save bitmap.")
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("BitmapSaver", "Error saving bitmap: ${e.message}")
            uri?.let { resolver.delete(it, null, null) }
            null
        }
    }
}
```

---

## 🎨 Configuration

Customize the UI and detection behavior using a `FaceVerificationConfig` object:

```kotlin
import com.aria.danesh.faceverificationlib.view.FaceVerificationConfig
import com.aria.danesh.faceverificationlib.view.defaultFaceVerificationConfig

val customConfig = defaultFaceVerificationConfig().copy(
    smileThreshold = 0.8f,   // Require a bigger smile (0.0–1.0)
    successColor = Color.Magenta,
    primaryColor = Color.Yellow,
    errorColor = Color.Cyan
)

FaceVerificationComponent(
    onSmileDetected = { /* Handle smile */ },
    config = customConfig
)
```

| Parameter | Type | Default | Description |
|------------|-------|----------|-------------|
| `smileThreshold` | `Float` | `0.7f` | Minimum probability for smile detection (0.0–1.0). |
| `successColor` | `Color` | `Color.Green` | Color used when a smile is detected. |
| `primaryColor` | `Color` | `MaterialTheme.colorScheme.primary` | Default scanning overlay color. |
| `errorColor` | `Color` | `MaterialTheme.colorScheme.error` | Color used when multiple or no faces are found. |

---

## 🤝 Contributing

Contributions are welcome!
- 🐛 Found a bug? [Open an issue](../../issues).
- 💡 Have a feature idea? Let’s discuss it!
- 🔧 Want to improve the code? Submit a pull request.

---

## 📄 License

```
Copyright 2025 Aria Danesh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0
```

---

## 🌐 Author

Developed by **[Aria Danesh](https://arya458.github.io/AndroidFaceVerificationLibrary/)**
> Passionate about modern Android development, open-source tools, and accessible AI-driven user experiences.
