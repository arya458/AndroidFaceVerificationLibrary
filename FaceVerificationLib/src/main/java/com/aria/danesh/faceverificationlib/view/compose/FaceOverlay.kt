package com.aria.danesh.faceverificationlib.view.compose

import androidx.annotation.DrawableRes
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.aria.danesh.faceverificationlib.view.compose.UniversalData.isFrontCamera
import com.google.mlkit.vision.face.Face

object UniversalData{

    var isFrontCamera = false

}

@Composable
fun FaceOverlay(
    detectedFace: Face?,
    @DrawableRes frameDrawableId: Int,
    imageProxy: ImageProxy?,
    color: Color
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    imageProxy?.let { proxy ->
        val imageWidth = proxy.width
        val imageHeight = proxy.height

        detectedFace?.boundingBox?.let { rect ->
            val viewWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
            val viewHeightPx = context.resources.displayMetrics.heightPixels.toFloat()

            val scaleX = viewWidthPx / imageHeight.toFloat() // Notice the swap for landscape
            val scaleY = viewHeightPx / imageWidth.toFloat()  // Notice the swap for landscape

            val originalLeft = rect.left.toFloat()
            val originalRight = rect.right.toFloat()

            val mirroredLeft = if (isFrontCamera) {
                viewWidthPx - originalRight * scaleX
            } else {
                originalLeft * scaleX
            }

            val mirroredRight = if (isFrontCamera) {
                viewWidthPx - originalLeft * scaleX
            } else {
                originalRight * scaleX
            }

            val scaledTop = rect.top * scaleY
            val scaledBottom = rect.bottom * scaleY

            val heightDp = with(density) { (scaledBottom - scaledTop).toDp() }
            val widthDp = with(density) { (mirroredRight - mirroredLeft).toDp() }
            val offsetXDp = with(density) { mirroredLeft.toDp() }
            val offsetYDp = with(density) { scaledTop.toDp() }

            Image(
                painter = painterResource(id = frameDrawableId),
                contentDescription = "Face Frame",
                modifier = Modifier
                    .size(height = heightDp, width = widthDp)
                    .absoluteOffset(x = offsetXDp, y = offsetYDp),
                colorFilter = ColorFilter.tint(color)
            )
        }
    }
}