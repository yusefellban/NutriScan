package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R
import java.util.Locale

/**
 * Steps card with an open (123°→417°) gauge ring showing progress toward [stepsGoal].
 * Track and fill are both drawn natively via [Canvas]. Once the goal is reached,
 * the inner text and footsteps icon switch to the same teal as the fill arc.
 */
@Composable
fun StepsGaugeCard(
    steps: Int,
    stepsGoal: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = AppTheme.isDark
    val trackColor = AppTheme.colors.CaloriesMutedTeal
    val fillColor = AppTheme.colors.Teal700
    val goalReached = stepsGoal > 0 && steps >= stepsGoal
    val innerTextColor = when {
        goalReached -> fillColor
        isDark -> AppTheme.colors.CaloriesMutedTeal
        else -> AppTheme.colors.Gray700
    }
    val innerValueColor = when {
        goalReached -> fillColor
        isDark -> AppTheme.colors.CaloriesMutedTeal
        else -> AppTheme.colors.Gray1600
    }

    val targetProgress = if (stepsGoal > 0) (steps.toFloat() / stepsGoal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400),
        label = "steps_progress",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .calorieCardSurface(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(94.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(94.dp)) {
                drawArc(
                    color = trackColor,
                    startAngle = 123f,
                    sweepAngle = 294f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Canvas(modifier = Modifier.size(94.dp)) {
                drawArc(
                    color = fillColor,
                    startAngle = 123f,
                    sweepAngle = 294f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.steps),
                    style = CaloriesTypography.badgeText,
                    color = innerTextColor,
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d", steps),
                    style = AppTheme.typography.titleMedium,
                    color = innerValueColor,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.steps),
            contentDescription = null,
            tint = if (goalReached) fillColor else Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
    }
}
