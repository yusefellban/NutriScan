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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SoundwaveHeightRuler(
    currentHeightDouble: Double,
    minHeight: Int,
    maxHeight: Int,
    tickSpacingPx: Float,
    gearRatio: Float,
    isDraggingRuler: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    onDragScroll: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = isSystemInDarkTheme()

    val selectedTickColor = AppTheme.colors.Teal1000
    val unselectedTickColor = if (isDark) Color.White else AppTheme.colors.HeightRulerMinorTick
    val unselectedTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else AppTheme.colors.HeightRulerText

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
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
        val startHeight = (currentHeightDouble - (centerX / tickSpacingPx)).toInt().coerceAtLeast(minHeight)
        val endHeight = (currentHeightDouble + (centerX / tickSpacingPx)).toInt().coerceAtMost(maxHeight)

        // Draw ticks and labels
        for (h in startHeight..endHeight) {
            val x = centerX + (h - currentHeightDouble).toFloat() * tickSpacingPx

            // Calculate distance from the center in number of ticks
            val dist = (h - currentHeightDouble).toFloat().absoluteValue
            val maxDistance = (centerX / tickSpacingPx).coerceAtLeast(1f)
            val fraction = (1f - (dist / maxDistance)).coerceIn(0f, 1f)

            // Smooth wave factor (parabolic/bell curve decay)
            val waveFactor = fraction * fraction

            // Check major vs minor tick (4 short ticks, then 1 taller)
            val isMajor = h % 5 == 0
            val baseHeightDp = if (isMajor) 32.dp else 16.dp
            val scale = 0.8f + 0.4f * waveFactor
            val tickHeight = baseHeightDp.toPx() * scale

            // Check if this tick is selected
            val isSelectedTick = h == currentHeightDouble.roundToInt()
            val tickColor = if (isSelectedTick) selectedTickColor else unselectedTickColor
            val strokeWidth = 2.dp.toPx()

            val centerY = size.height / 2f
            val startY = centerY - tickHeight / 2f
            val endY = centerY + tickHeight / 2f

            drawLine(
                color = tickColor,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = strokeWidth
            )

            if (isMajor) {
                val labelTextColor = if (isSelectedTick) {
                    selectedTickColor
                } else {
                    unselectedTextColor
                }

                val textLayoutResult = textMeasurer.measure(
                    text = h.toString(),
                    style = TextStyle(
                        color = labelTextColor,
                        fontSize = 15.sp,
                        fontFamily = LexendDeca,
                        fontWeight = if (isSelectedTick) FontWeight.Bold else FontWeight.Medium
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = x - textLayoutResult.size.width / 2f,
                        y = size.height - 18.dp.toPx()
                    )
                )
            }
        }
    }
}
