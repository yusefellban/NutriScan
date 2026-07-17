package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WeightArcRuler(
    currentWeightDouble: Double,
    minWeight: Int,
    maxWeight: Int,
    tickSpacingPx: Float,
    gearRatio: Float,
    onDraggingChanged: (Boolean) -> Unit,
    onDragScroll: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = isSystemInDarkTheme()

    val selectedTickColor = AppTheme.colors.Teal1000
    val unselectedTickColor = if (isDark) {
        Color.White.copy(alpha = 0.5f)
    } else {
        AppTheme.colors.HeightRulerMinorTick
    }
    val unselectedTextColor = if (isDark) {
        Color.White.copy(alpha = 0.7f)
    } else {
        AppTheme.colors.HeightRulerText
    }
    val arcTrackColor = if (isDark) {
        AppTheme.colors.HeightRulerMinorTick.copy(alpha = 0.4f)
    } else {
        AppTheme.colors.HeightRulerMinorTick
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onDraggingChanged(true) },
                    onDragEnd = {
                        onDraggingChanged(false)
                        onDragEnd()
                    },
                    onDragCancel = { onDraggingChanged(false) }
                ) { change, dragAmount ->
                    change.consume()
                    onDragScroll(-dragAmount * gearRatio)
                }
            }
    ) {
        val centerX = size.width / 2f
        val arcCenterY = size.height - 8.dp.toPx()
        val radius = size.width * 0.42f
        val degreesPerKg = 2.8f
        val centerAngleRad = (PI / 2).toFloat()

        // Background arc track
        drawArc(
            color = arcTrackColor,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - radius, arcCenterY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        val visibleRange = (centerX / tickSpacingPx).toInt().coerceAtLeast(10)
        val startWeight = (currentWeightDouble - visibleRange).toInt().coerceAtLeast(minWeight)
        val endWeight = (currentWeightDouble + visibleRange).toInt().coerceAtMost(maxWeight)

        for (w in startWeight..endWeight) {
            val deltaKg = w - currentWeightDouble
            val angleRad = centerAngleRad + Math.toRadians(deltaKg * degreesPerKg).toFloat()

            val tickBaseX = centerX + radius * cos(angleRad)
            val tickBaseY = arcCenterY - radius * sin(angleRad)

            val dist = abs(deltaKg).toFloat()
            val maxDistance = visibleRange.toFloat().coerceAtLeast(1f)
            val fraction = (1f - (dist / maxDistance)).coerceIn(0f, 1f)
            val waveFactor = fraction * fraction

            val isMajor = w % 10 == 0
            val baseTickLength = if (isMajor) 28.dp.toPx() else 14.dp.toPx()
            val tickLength = baseTickLength * (0.7f + 0.3f * waveFactor)

            val isSelectedTick = w == currentWeightDouble.roundToInt()
            val tickColor = if (isSelectedTick) selectedTickColor else unselectedTickColor
            val strokeWidth = if (isSelectedTick) 3.dp.toPx() else 2.dp.toPx()

            // Radial tick direction (pointing inward toward arc center)
            val dirX = cos(angleRad)
            val dirY = -sin(angleRad)

            drawLine(
                color = tickColor,
                start = Offset(tickBaseX, tickBaseY),
                end = Offset(
                    tickBaseX + dirX * tickLength,
                    tickBaseY + dirY * tickLength
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            if (isMajor) {
                val labelTextColor = if (isSelectedTick) selectedTickColor else unselectedTextColor
                val textLayoutResult = textMeasurer.measure(
                    text = w.toString(),
                    style = TextStyle(
                        color = labelTextColor,
                        fontSize = 15.sp,
                        fontFamily = LexendDeca,
                        fontWeight = if (isSelectedTick) FontWeight.Bold else FontWeight.Medium
                    )
                )
                val labelOffset = tickLength + 6.dp.toPx()
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = tickBaseX + dirX * labelOffset - textLayoutResult.size.width / 2f,
                        y = tickBaseY + dirY * labelOffset - textLayoutResult.size.height / 2f
                    )
                )
            }
        }
    }
}
