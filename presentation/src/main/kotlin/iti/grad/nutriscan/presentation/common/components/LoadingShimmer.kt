package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import iti.grad.nutriscan.presentation.common.theme.AppTheme

/**
 * Creates a generic shimmer loading effect.
 * The shimmer sweeps across the modifier it's attached to.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    
    val isDark = AppTheme.isDark
    
    val shimmerColors = if (isDark) {
        listOf(
            AppTheme.colors.Gray700.copy(alpha = 0.9f),
            AppTheme.colors.Gray500.copy(alpha = 1f),
            AppTheme.colors.Gray700.copy(alpha = 0.9f),
        )
    } else {
        listOf(
            AppTheme.colors.Gray200.copy(alpha = 0.6f),
            AppTheme.colors.Gray100.copy(alpha = 0.2f),
            AppTheme.colors.Gray200.copy(alpha = 0.6f),
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffsetX"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
    .onGloballyPositioned {
        size = it.size
    }
}
