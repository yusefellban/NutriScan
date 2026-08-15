package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.nutriscan.presentation.common.util.tick
import iti.grad.presentation.R
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Solid Teal500 card showing TDEE vs. calories gained, with a rounded-cap
 * linear progress bar and a mood indicator for how close to goal the user is.
 * Fixed Teal500/Teal100/Teal1600/Teal300/Teal1300 palette — this card is
 * intentionally identical in both light and dark mode per the design spec.
 */
@Composable
fun CalorieGoalsCard(
    tdee: Int,
    caloriesGained: Int,
    caloriesBurned: Int,
    modifier: Modifier = Modifier,
) {
    val trackColor = AppTheme.colors.Teal300
    val moodRatio = if (tdee > 0) caloriesGained.toFloat() / tdee else 0f
    val exceeded = moodRatio > 1f
    // Fixed light-red, not a mode-aware token — this card is intentionally the same
    // palette in both light and dark mode (see doc comment above).
    val fillColor = if (exceeded) Color(0xFFFF8A80) else AppTheme.colors.Teal1000
    val progress = moodRatio.coerceIn(0f, 1f)
    val moodDrawable = when {
        exceeded -> R.drawable.angry
        moodRatio >= 0.8f -> R.drawable.happy
        moodRatio >= 0.5f -> R.drawable.normal
        else -> R.drawable.sad
    }

    val faceBounce = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Idle motion so the mascot reads as "alive" even without a tap — pace and shape tuned
    // per mood: angry shakes fast, happy bobs cheerfully, normal breathes slowly, sad droops.
    val idleTransition = rememberInfiniteTransition(label = "moodIdle")
    val idlePhase by idleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    exceeded -> 220
                    moodRatio >= 0.8f -> 550
                    moodRatio >= 0.5f -> 1400
                    else -> 2000
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idlePhase",
    )
    val idleTranslationY = when {
        exceeded -> 0f
        moodRatio >= 0.8f -> -6f * idlePhase
        moodRatio >= 0.5f -> 0f
        else -> 3f * idlePhase
    }
    val idleRotationZ = if (exceeded) (idlePhase - 0.5f) * 10f else 0f
    val idleScale = if (!exceeded && moodRatio in 0.5f..0.8f) 1f + 0.03f * idlePhase else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.Large)
            .background(AppTheme.colors.Teal500)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.tick()
                scope.launch {
                    repeat(2) {
                        faceBounce.animateTo(-14f, tween(150, easing = FastOutSlowInEasing))
                        faceBounce.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
                    }
                }
            }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calorie),
                        contentDescription = null,
                        tint = AppTheme.colors.CaloriesIconOnAccent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.calorie_goals),
                        style = CaloriesTypography.sectionTitle,
                        color = AppTheme.colors.Teal100,
                    )
                }

                val kcalUnit = stringResource(R.string.kcal_suffix)
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    GoalStatRow(
                        label = stringResource(R.string.your_tdee),
                        value = "${formatKcal(tdee)} $kcalUnit",
                        badgeColor = trackColor,
                    )
                    GoalStatRow(
                        label = stringResource(R.string.calories_gained),
                        value = "${formatKcal(caloriesGained)} $kcalUnit",
                        badgeColor = trackColor,
                    )
                    GoalStatRow(
                        label = stringResource(R.string.calories_burned),
                        value = "${formatKcal(caloriesBurned)} $kcalUnit",
                        badgeColor = trackColor,
                    )
                }
            }

            Icon(
                painter = painterResource(moodDrawable),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        translationY = faceBounce.value + idleTranslationY
                        rotationZ = idleRotationZ
                        scaleX = idleScale
                        scaleY = idleScale
                    },
            )
        }

        Spacer(modifier = Modifier.height(38.dp))

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)) {
            val strokeWidthPx = size.height
            val y = size.height / 2f
            drawLine(
                color = trackColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = fillColor,
                start = Offset(0f, y),
                end = Offset(size.width * progress, y),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun GoalStatRow(
    label: String,
    value: String,
    badgeColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.Teal1600,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor)
                .padding(horizontal = 3.dp),
        ) {
            Text(
                text = value,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.Teal1300,
            )
        }
    }
}

private fun formatKcal(value: Int): String = String.format(Locale.getDefault(), "%,d", value)
