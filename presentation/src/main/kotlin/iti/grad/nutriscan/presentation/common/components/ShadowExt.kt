package iti.grad.nutriscan.presentation.common.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Custom shadow modifier to provide a uniform glowing shadow or directionally offset shadow.
 * This circumvents the standard Android elevation shadow which forces a downward light source.
 */
fun Modifier.customShadow(
    shape: Shape,
    color: Color,
    blurRadius: Float = 60f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = Color.Transparent.toArgb()
        frameworkPaint.setShadowLayer(
            blurRadius,
            offsetX,
            offsetY,
            color.toArgb()
        )

        when (outline) {
            is Outline.Generic -> canvas.drawPath(outline.path, paint)
            is Outline.Rounded -> canvas.drawRoundRect(
                outline.roundRect.left, outline.roundRect.top,
                outline.roundRect.right, outline.roundRect.bottom,
                outline.roundRect.topLeftCornerRadius.x,
                outline.roundRect.topLeftCornerRadius.y,
                paint
            )
            is Outline.Rectangle -> canvas.drawRect(outline.rect, paint)
        }
    }
}
