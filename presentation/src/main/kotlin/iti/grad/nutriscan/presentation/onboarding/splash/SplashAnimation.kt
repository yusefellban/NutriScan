package iti.grad.nutriscan.presentation.onboarding.splash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter

/**
 * Draws the two organic blurred blob circles that form the background animation layer
 * of the Splash Screen.
 *
 * # Design Intent (mirroring iOS)
 * Two circular gradient blobs start near the TOP-RIGHT of the screen and drift
 * diagonally toward the BOTTOM-LEFT. They carry the teal gradient that is slowly
 * "pushed off screen" as the background transitions to its final color.
 *
 * ## Why Canvas + BlurMaskFilter instead of Modifier.blur?
 * [androidx.compose.ui.draw.blur] requires API 31 (Android 12).
 * [BlurMaskFilter] works on all API levels and is the production-correct solution
 * for this project's minSdk = 30.
 *
 * @param progress Animation progress from 0f (start) to 1f (end).
 *   - At 0f blobs are positioned TOP-RIGHT
 *   - At 1f blobs are positioned BOTTOM-LEFT (off-screen or barely visible)
 * @param blob1Color1 Gradient start color for blob 1
 * @param blob1Color2 Gradient end color for blob 1
 * @param blob2Color1 Gradient start color for blob 2
 * @param blob2Color2 Gradient end color for blob 2
 * @param modifier Modifier applied to the Canvas (typically fillMaxSize + ignoresSafeArea)
 */
@Composable
fun SplashBlobLayer(
    progress: Float,
    blob1Color1: Color,
    blob1Color2: Color,
    blob2Color1: Color,
    blob2Color2: Color,
    modifier: Modifier = Modifier,
) {
    // -----------------------------------------------------------------------
    // PERFORMANCE OPTIMIZATION: 
    // Remember Paint and BlurMaskFilter allocations. Doing this inside the Canvas
    // on every frame causes massive GC churn, dropping frames and delaying startup.
    // -----------------------------------------------------------------------
    val blob1Paint = androidx.compose.runtime.remember {
        Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }
    
    val blob2Paint = androidx.compose.runtime.remember {
        Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    Canvas(modifier = modifier) {
        drawBlob1(progress, blob1Color1, blob1Color2, blob1Paint)
        drawBlob2(progress, blob2Color1, blob2Color2, blob2Paint)
    }
}

// ---------------------------------------------------------------------------
// Blob 1 — Large blob (1.3× screen width), starts top-left, ends bottom-right
// ---------------------------------------------------------------------------

private fun DrawScope.drawBlob1(
    progress: Float,
    color1: Color,
    color2: Color,
    paint: Paint,
) {
    val radius = size.width * 0.65f  // half of 1.3× width

    // Start: top-left (off-screen-left). End: bottom-right (off-screen-right).
    val startX = -size.width * 0.2f  
    val startY = -size.height * 0.1f 
    val endX = size.width * 1.2f    
    val endY = size.height * 1.0f    

    val cx = lerp(startX, endX, progress)
    val cy = lerp(startY, endY, progress)

    // Scale: 0.8 → 1.1
    val scale = lerp(0.8f, 1.1f, progress)
    val effectiveRadius = radius * scale

    // Blurred radial gradient paint
    drawIntoCanvas { canvas ->
        paint.asFrameworkPaint().shader = android.graphics.RadialGradient(
            cx, cy,
            effectiveRadius,
            intArrayOf(color1.toArgb(), color2.toArgb()),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            center = Offset(cx, cy),
            radius = effectiveRadius,
            paint = paint,
        )
    }
}

// ---------------------------------------------------------------------------
// Blob 2 — Smaller blob (0.9× screen width), complementary movement
// ---------------------------------------------------------------------------

private fun DrawScope.drawBlob2(
    progress: Float,
    color1: Color,
    color2: Color,
    paint: Paint,
) {
    val radius = size.width * 0.45f  // half of 0.9× width

    // Start: top-left. End: bottom-right.
    val startX = -size.width * 0.011f
    val startY = -size.height * 0.011f
    val endX = size.width * 1.5f
    val endY = size.height * 1.1f

    val cx = lerp(startX, endX, progress)
    val cy = lerp(startY, endY, progress)

    // Scale: 0.7 → 1.0
    val scale = lerp(0.5f, 1.4f, progress)
    val effectiveRadius = radius * scale

    drawIntoCanvas { canvas ->
        paint.asFrameworkPaint().shader = android.graphics.RadialGradient(
            cx, cy,
            effectiveRadius,
            intArrayOf(color1.toArgb(), color2.toArgb()),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            center = Offset(cx, cy),
            radius = effectiveRadius,
            paint = paint,
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Linear interpolation — avoids importing androidx.compose.ui.util.lerp here. */
private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)
