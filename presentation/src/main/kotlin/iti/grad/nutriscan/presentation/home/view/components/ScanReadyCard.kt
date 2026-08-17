package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

private const val GlowPulseCycleMillis = 1600

/** Dashed-border scan-ready CTA card displayed on the Home screen. */
@Composable
fun ScanReadyCard(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val borderColor = AppTheme.colors.Teal500
    // The dark surface swallows the light Teal300 glow used on light backgrounds, so dark
    // mode needs a brighter, higher-alpha core to read as a glow instead of a faint smudge.
    val isDark = AppTheme.isDark
    val glowColor = if (isDark) AppTheme.colors.Teal300 else AppTheme.colors.Teal500
    val glowPeakAlpha = 0.38f

    // Soft breathing glow across the whole card — grows/shrinks continuously, no held/frozen state.
    val glowTransition = rememberInfiniteTransition(label = "scanReadyGlow")
    val glowPulse by
            glowTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation =
                                            tween(
                                                    GlowPulseCycleMillis,
                                                    easing = FastOutSlowInEasing
                                            ),
                                    repeatMode = RepeatMode.Reverse,
                            ),
                    label = "glowPulse",
            )

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .customShadow(
                                    shape = RoundedCornerShape(21.dp),
                                    color = AppTheme.colors.Teal700.copy(alpha = 0.3f),
                                    blurRadius = 100f,
                                    offsetY = 0f
                            )
                            .background(AppTheme.colors.Surface, RoundedCornerShape(21.dp))
                            .drawBehind {
                                drawRoundRect(
                                        color = borderColor,
                                        cornerRadius = CornerRadius(21.dp.toPx()),
                                        style =
                                                Stroke(
                                                        width = 2.dp.toPx(),
                                                        pathEffect =
                                                                PathEffect.dashPathEffect(
                                                                        intervals =
                                                                                floatArrayOf(
                                                                                        7.5f.dp
                                                                                                .toPx(),
                                                                                        5.dp.toPx()
                                                                                ),
                                                                        phase = 0f,
                                                                ),
                                                ),
                                )
                            }
                            .clip(RoundedCornerShape(21.dp))
                            .drawBehind {
                                val radius = size.width * (0.6f + 0.3f * glowPulse)
                                val alpha = glowPeakAlpha * (0.5f + 0.5f * glowPulse)
                                drawCircle(
                                        brush =
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        glowColor.copy(
                                                                                alpha = alpha
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center = center,
                                                        radius = radius,
                                                ),
                                        radius = radius,
                                        center = center,
                                )
                            }
                            .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = AppTheme.colors.Primary),
                                    onClick = onClick,
                            )
                            .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
    ) {
        Icon(
                painter = painterResource(R.drawable.ic_qr_scan),
                contentDescription = stringResource(R.string.home_ready_to_scan),
                tint = AppTheme.colors.HealthSubtitleColor,
                modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
                text = stringResource(R.string.home_ready_to_scan),
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.HealthSubtitleColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = stringResource(R.string.home_scan_subtitle),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.Teal800,
        )
    }
}
