package iti.grad.nutriscan.presentation.common.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Draws the bottom-nav-bar background: rounded outer corners, a fixed center cutout for the
 * floating Scan button ("big notch"), and a small sliding cutout beneath the selected tab
 * ("small notch"). Ported 1:1 from the iOS `TabBarCurveShape`, including its exact bezier
 * control points — only rescaled from raw point values to dp via [Density] so it renders
 * identically across screen densities.
 *
 * @param curveX Horizontal center (px) of the small, moving selection-indicator notch.
 * @param notchX Horizontal center (px) of the large, fixed Scan-button notch.
 * @param hideSmallNotch When true, only the big notch is drawn (Scan tab selected, or the
 * small notch has slid close enough to the big notch that drawing both would overlap).
 * @param cornerRadius Radius of the bar's outer top corners.
 */
class BottomNavCurveShape(
    private val curveX: Float,
    private val notchX: Float,
    private val hideSmallNotch: Boolean,
    private val cornerRadius: Dp = 22.dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        with(density) {
            val width = size.width
            val height = size.height
            val cr = cornerRadius.toPx()
            // Bezier control offset for a quarter-circle corner (kappa complement), matches the
            // iOS shape's hard-coded 14.33/32 ratio for its 32pt corner radius.
            val crControl = cr * 0.4478f
            val bigNotchHalfWidth = 60.115f.dp.toPx()
            val smallNotchHalfWidth = 15.25f.dp.toPx()

            path.moveTo(0f, height)
            path.lineTo(0f, cr)
            path.cubicTo(0f, crControl, crControl, 0f, cr, 0f)

            // Draw notches left-to-right based on their current horizontal order.
            when {
                hideSmallNotch -> {
                    path.lineTo(notchX - bigNotchHalfWidth, 0f)
                    addBigNotch(path, notchX)
                }
                curveX < notchX -> {
                    path.lineTo(curveX - smallNotchHalfWidth, 0f)
                    addSmallNotch(path, curveX)
                    if (curveX + smallNotchHalfWidth < notchX - bigNotchHalfWidth) {
                        path.lineTo(notchX - bigNotchHalfWidth, 0f)
                    }
                    addBigNotch(path, notchX)
                }
                else -> {
                    path.lineTo(notchX - bigNotchHalfWidth, 0f)
                    addBigNotch(path, notchX)
                    if (notchX + bigNotchHalfWidth < curveX - smallNotchHalfWidth) {
                        path.lineTo(curveX - smallNotchHalfWidth, 0f)
                    }
                    addSmallNotch(path, curveX)
                }
            }

            path.lineTo(width - cr, 0f)
            path.cubicTo(width - crControl, 0f, width, crControl, width, cr)
            path.lineTo(width, height)
            path.close()
        }
        return Outline.Generic(path)
    }

    /** The large, fixed cutout the floating Scan button sits in. */
    private fun Density.addBigNotch(path: Path, center: Float) {
        fun px(v: Float) = v.dp.toPx()

        path.cubicTo(
            center - px(49.005f), 0f,
            center - px(39.995f), px(9.01f),
            center - px(39.995f), px(20.12f),
        )
        path.cubicTo(
            center - px(39.995f), px(31.23f),
            center - px(30.985f), px(40.25f),
            center - px(19.875f), px(40.25f),
        )
        path.lineTo(center + px(19.875f), px(40.25f))
        path.cubicTo(
            center + px(30.985f), px(40.25f),
            center + px(39.995f), px(31.24f),
            center + px(39.995f), px(20.13f),
        )
        path.cubicTo(
            center + px(39.995f), px(9.02f),
            center + px(49.005f), 0f,
            center + px(60.115f), 0f,
        )
    }

    /** The small, sliding cutout that follows the selected tab. */
    private fun Density.addSmallNotch(path: Path, center: Float) {
        fun px(v: Float) = v.dp.toPx()
        fun nx(offset: Float) = center + px(offset)

        path.cubicTo(nx(-11.92f), 0f, nx(-9.01f), px(1.86f), nx(-7.52f), px(4.60f))
        path.cubicTo(nx(-5.97f), px(7.14f), nx(-3.18f), px(8.83f), nx(0.00f), px(8.83f))
        path.cubicTo(nx(3.18f), px(8.83f), nx(5.97f), px(7.14f), nx(7.52f), px(4.60f))
        path.cubicTo(nx(7.59f), px(4.47f), nx(7.66f), px(4.35f), nx(7.73f), px(4.23f))
        path.cubicTo(nx(9.27f), px(1.69f), nx(12.06f), 0f, nx(15.24f), 0f)
    }
}
