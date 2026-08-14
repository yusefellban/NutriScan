package iti.grad.nutriscan.presentation.common.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import kotlin.math.abs

/**
 * Rectangular product/content thumbnail that picks Fit over Crop when the
 * source image's aspect ratio is already close to the box's — so a
 * near-matching photo shows uncropped, and only a mismatched one falls back
 * to filling the box (and losing its edges).
 */
@Composable
fun AdaptiveAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentScale by remember { mutableStateOf(ContentScale.Crop) }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier.onGloballyPositioned { containerSize = it.size },
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        onSuccess = { state ->
            val imageSize = state.painter.intrinsicSize
            contentScale = if (
                containerSize.width > 0 && containerSize.height > 0 &&
                imageSize.width > 0 && imageSize.height > 0
            ) {
                val containerRatio = containerSize.width.toFloat() / containerSize.height
                val imageRatio = imageSize.width / imageSize.height
                val ratioDiff = abs(containerRatio - imageRatio) / containerRatio
                if (ratioDiff < 0.2f) ContentScale.Fit else ContentScale.Crop
            } else {
                ContentScale.Crop
            }
        },
    )
}
