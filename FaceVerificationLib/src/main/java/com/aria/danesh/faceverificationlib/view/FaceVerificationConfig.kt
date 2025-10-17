package com.aria.danesh.faceverificationlib.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aria.danesh.faceverificationlib.R

/**
 * Configuration for the FaceVerificationComponent.
 *
 * @param smileThreshold The minimum probability for a smile to be considered detected (0.0 to 1.0).
 * @param successColor The color used for the overlay and icon on successful detection.
 * @param primaryColor The color used for the overlay and icon during normal scanning.
 * @param errorColor The color used for the overlay and icon when an error occurs.
 */
data class FaceVerificationConfig(
    val smileThreshold: Float = 0.7f,
    val successColor: Color,
    val primaryColor: Color,
    val errorColor: Color
)

@Composable
fun defaultFaceVerificationConfig() = FaceVerificationConfig(
    smileThreshold = 0.7f,
    successColor = Color.Green,
    primaryColor = MaterialTheme.colorScheme.primary,
    errorColor = MaterialTheme.colorScheme.error
)
