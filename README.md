# Android Face Verification Library

A robust and easy-to-integrate solution for face verification within Android applications. This library leverages the device's camera and advanced face detection and recognition techniques to enable secure and seamless user authentication and identification.

**Author:** Aria Danesh

## Features

- Real-time face detection and verification
- Camera integration using AndroidX CameraX
- Face detection using ML Kit
- Modern UI components built with Jetpack Compose
- Smooth animations using Lottie
- Material Design 3 components
- Minimum SDK version: 24 (Android 7.0)

## 🛠️ Installation - Get Started in Minutes!

Add the JitPack repository to your root `build.gradle` file:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' } // Add JitPack
    }
}
```

Then, add the library dependency to your app's build.gradle file:

```gradle
dependencies {
     implementation 'com.github.arya458:AndroidFaceVerificationLibrary:1.0.0' // Replace with the latest version
    // ... other dependencies
}
```

### Required Permissions

Add these permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-feature android:name="android.hardware.camera" />
```

**Important:** Ensure you handle runtime permission requests in your application code.

## 🕹️ Usage - Simple as a Smile!

### Basic Implementation

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aria.danesh.faceverificationlib.view.FaceVerificationComponent
import com.aria.danesh.faceverificationlib.utils.ImageProxyHelperKt.imageProxyToBase64

@Composable
fun MyVerificationScreen() {
    val context = LocalContext.current
    val faceDetectionCallback = remember {
        object : FaceVerificationComponent.FaceDetectionCallback {
            override fun onOneFaceDetected(imageProxy: androidx.camera.core.ImageProxy) {
                // One face detected! Process the imageProxy here.
                val base64Image = imageProxyToBase64(imageProxy)
                base64Image?.let {
                    android.util.Log.d("FaceVerification", "Base64 Image (first 50 chars): ${it.substring(0, 50)}...")
                    // Send to your verification backend or process locally
                }
                imageProxy.close() // Remember to close the ImageProxy
            }

            override fun onNoFaceDetected() {
                android.util.Log.d("FaceVerification", "No face detected")
                // Handle no face detected scenario
            }

            override fun onMultipleFacesDetected() {
                android.util.Log.d("FaceVerification", "Multiple faces detected")
                // Handle multiple faces detected scenario
            }
        }
    }

    FaceVerificationComponent(
        context = context,
        faceDetectionCallback = faceDetectionCallback
    )
}
```

### Complete Example with Image Capture

Here's a complete example showing how to implement face verification with image capture and saving:

```kotlin
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
                    onSmile = { imageProxy ->
                        // Handle smile detection
                        Log.d("Image", "onSmile: ${imageProxyToBase64(imageProxy)}")
                    },
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
                    }
                )

                // Save captured image
                bitmap?.let {
                    val filename = "my_image_${System.currentTimeMillis()}.jpg"
                    val uri = bitmapSaver(it, Bitmap.CompressFormat.JPEG, 90, filename)
                    if (uri != null) {
                        Log.d("BitmapSaver", "Image saved at: $uri")
                    } else {
                        Log.e("BitmapSaver", "Failed to save image")
                    }
                }
            }
        }
    }
}
```

## 📂 Repository Structure - Organized for Clarity

```
FaceVerification/
├── .gitignore
├── LICENSE
├── README.md
├── app/                    # Example application showcasing the library
│   └── ...
├── faceverificationlib/    # The core library module
│   ├── build.gradle.kts
│   ├── src/main/java/com/aria/danesh/faceverificationlib/
│   │   ├── managers/         # CameraX setup and face detection logic
│   │   │   └── FaceDetectionManager.kt
│   │   ├── utils/            # Helper functions (e.g., ImageProxy to Base64)
│   │   │   └── ImageProxyHelperKt.kt
│   │   └── view/             # Jetpack Compose UI component
│   │       └── FaceVerificationComponent.kt
└── logo/                   # Logo image
```

## 💡 Potential Future Enhancements

- Face Recognition: Implement face recognition capabilities to identify specific individuals
- Liveness Detection: Add features to prevent spoofing attacks
- Customizable Detection Parameters: Allow developers to configure face detection sensitivity and other parameters
- More UI Customization Options: Provide more flexibility in styling the UI components
- Error Handling Improvements: Enhance error reporting and handling mechanisms

## ❤️ Contributing - Let's Build Together!

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

You can contribute in several ways:

- Bug Reports: If you find any issues, please report them clearly and concisely
- Feature Requests: Have a cool idea for a new feature? Open an issue to discuss it
- Pull Requests: Feel free to submit pull requests with bug fixes or new features. Please ensure your code follows the project's coding style

## 🛡️ License

This project is licensed under the Apache License 2.0.

## 🙏 Acknowledgements

A big thank you to:
- AndroidX Team
- ML Kit Team
- Jetpack Compose Team
- Lottie Team

Made with ❤️ by Aria Danesh 
