package iti.grad.nutriscan.presentation.common.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom shadow modifier to provide a uniform glowing shadow or directionally offset shadow.
 * This circumvents the standard Android elevation shadow which forces a downward light source.
 *
 * [spread] shrinks the shadow-casting outline inward before blurring (matching CSS
 * box-shadow's negative spread), producing a tighter shadow than the raw shape bounds.
 */
fun Modifier.customShadow(
    shape: Shape,
    color: Color,
    blurRadius: Float = 60f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    spread: Dp = 0.dp,
) = this.drawBehind {
    val spreadPx = spread.toPx()
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
                outline.roundRect.left + spreadPx, outline.roundRect.top + spreadPx,
                outline.roundRect.right - spreadPx, outline.roundRect.bottom - spreadPx,
                (outline.roundRect.topLeftCornerRadius.x - spreadPx).coerceAtLeast(0f),
                (outline.roundRect.topLeftCornerRadius.y - spreadPx).coerceAtLeast(0f),
                paint
            )
            is Outline.Rectangle -> canvas.drawRect(
                outline.rect.left + spreadPx, outline.rect.top + spreadPx,
                outline.rect.right - spreadPx, outline.rect.bottom - spreadPx,
                paint
            )
        }
    }
}
