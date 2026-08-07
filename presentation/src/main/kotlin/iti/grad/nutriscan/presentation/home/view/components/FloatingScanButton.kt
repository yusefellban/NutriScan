package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlin.math.abs

private val ButtonSize = 60.dp
private val CornerRadius = 12.dp

/**
 * The floating center "Scan" tab button. Ported 1:1 from the iOS `FloatingTabButton`:
 * - A subtle idle "breathing" wobble on the whole button (rotate ±1.5°, scale up to +4%).
 * - A stronger idle wobble on just the icon (rotate ±4°, scale up to +10%), slightly out of
 *   phase with the button's own wobble — exactly as in the Swift keyframe timings.
 * - A diagonal glass-shine sweep across the button while idle, screen-blended.
 * - Swaps to a plain camera icon with no animation once the Scan tab is active.
 *
 * All idle animations stop (freeze at rest) once [isSelected] is true, matching the Swift
 * `keyframeAnimator(repeating: !isSelected)`.
 */
@Composable
fun FloatingScanButton(
    isSelected: Boolean,
    isUploadMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val buttonSizePx = with(density) { ButtonSize.toPx() }
    val backgroundColor = AppTheme.colors.ScanButtonBackground
    val iconColor = AppTheme.colors.ScanButtonIconTint

    val containerTransition = rememberInfiniteTransition(label = "scanButtonWobble")
    val containerWobble by containerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5000
                0f at 0
                0f at 50
                1f at 1050 using FastOutSlowInEasing
                0f at 2050 using FastOutSlowInEasing
                0f at 2500
                0f at 2550
                -1f at 3550 using FastOutSlowInEasing
                0f at 4550 using FastOutSlowInEasing
                0f at 5000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "containerWobble",
    )
    val activeContainerWobble = if (isSelected) 0f else containerWobble

    Box(
        modifier = modifier
            .size(ButtonSize)
            .graphicsLayer {
                scaleX = 1f + (0.04f * abs(activeContainerWobble))
                scaleY = scaleX
                rotationZ = 1.5f * activeContainerWobble
            }
            .customShadow(
                shape = RoundedCornerShape(CornerRadius),
                color = Color.Black.copy(alpha = 0.15f),
                blurRadius = with(density) { 5.dp.toPx() },
                offsetY = with(density) { 5.dp.toPx() },
            )
            .clip(RoundedCornerShape(CornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isSelected) {
            ScanButtonGlassShine(buttonSizePx = buttonSizePx)
        }

        if (isSelected) {
            Icon(
                painter = painterResource(
                    if (isUploadMode) {
                        R.drawable.ic_plus
                    } else {
                        R.drawable.ic_camera_solid
                    },
                ),
                contentDescription = stringResource(
                    if (isUploadMode) {
                        R.string.scan_center_action_upload
                    } else {
                        R.string.scan_center_action_capture
                    },
                ),
                tint = iconColor,
                modifier = Modifier.size(28.dp),
            )
        } else {
            val iconTransition = rememberInfiniteTransition(label = "scanIconWobble")
            val iconWobble by iconTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 5000
                        0f at 0
                        1f at 1000 using FastOutSlowInEasing
                        0f at 2000 using FastOutSlowInEasing
                        0f at 2500
                        -1f at 3500 using FastOutSlowInEasing
                        0f at 4500 using FastOutSlowInEasing
                        0f at 5000
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "iconWobble",
            )
            Icon(
                painter = painterResource(R.drawable.ic_scan),
                contentDescription = stringResource(R.string.nav_scan),
                tint = iconColor,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        scaleX = 1f + (0.1f * abs(iconWobble))
                        scaleY = scaleX
                        rotationZ = 4f * iconWobble
                    },
            )
        }
    }
}

/**
 * Diagonal shimmer band swept across the button while idle — the "glass shine" reflection.
 * Ported from the Swift `LinearGradient` + `keyframeAnimator` overlay, screen-blended so it
 * only brightens the button underneath it rather than dimming with its transparent edges.
 */
@Composable
private fun BoxScope.ScanButtonGlassShine(buttonSizePx: Float) {
    val transition = rememberInfiniteTransition(label = "scanButtonShine")
    val shineOffset by transition.animateFloat(
        initialValue = -buttonSizePx * 2f,
        targetValue = -buttonSizePx * 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                (-buttonSizePx * 2f) at 0
                (-buttonSizePx * 2f) at 50
                (buttonSizePx * 1.5f) at 2050 using LinearEasing
                (-buttonSizePx * 2f) at 2051
                (-buttonSizePx * 2f) at 2500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shineOffset",
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(CornerRadius))
            .drawBehind {
                val bandWidth = buttonSizePx * 2f
                val brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.35f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.2f),
                        0.65f to Color.Transparent,
                    ),
                    start = Offset(shineOffset, 0f),
                    end = Offset(shineOffset + bandWidth, size.height),
                )
                drawRect(brush = brush, blendMode = BlendMode.Screen)
            },
    )
}
