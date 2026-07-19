package iti.grad.nutriscan.presentation.common.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class BottomNavCurveShape(
    private val cornerRadius: Float = 60f,
    private val cutoutRadius: Float = 100f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height

            // Start at top-left corner
            moveTo(0f, cornerRadius)
            
            // Top-left rounded corner
            arcTo(
                rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            val cutoutHalfWidth = cutoutRadius * 0.9f
            val cutoutStart = (width / 2f) - cutoutHalfWidth
            val cutoutEnd = (width / 2f) + cutoutHalfWidth
            val cutoutDepth = cutoutRadius * 0.75f
            val cornerR = 60f

            // Line to before cutout
            lineTo(cutoutStart - cornerR, 0f)

            // Top-left outer curve (dipping in)
            arcTo(
                rect = Rect(cutoutStart - (cornerR * 2), 0f, cutoutStart, cornerR * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line down into cutout
            lineTo(cutoutStart, cutoutDepth - cornerR)

            // Bottom-left inner curve
            arcTo(
                rect = Rect(cutoutStart, cutoutDepth - (cornerR * 2), cutoutStart + (cornerR * 2), cutoutDepth),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )

            // Line across the bottom
            lineTo(cutoutEnd - cornerR, cutoutDepth)

            // Bottom-right inner curve
            arcTo(
                rect = Rect(cutoutEnd - (cornerR * 2), cutoutDepth - (cornerR * 2), cutoutEnd, cutoutDepth),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )

            // Line up out of cutout
            lineTo(cutoutEnd, cornerR)

            // Top-right outer curve (flattening out)
            arcTo(
                rect = Rect(cutoutEnd, 0f, cutoutEnd + (cornerR * 2), cornerR * 2),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to top-right corner
            lineTo(width - cornerRadius, 0f)

            // Top-right rounded corner
            arcTo(
                rect = Rect(width - (cornerRadius * 2), 0f, width, cornerRadius * 2),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to bottom-right
            lineTo(width, height)
            
            // Line to bottom-left
            lineTo(0f, height)
            
            // Close path
            close()
        }
        return Outline.Generic(path)
    }
}
