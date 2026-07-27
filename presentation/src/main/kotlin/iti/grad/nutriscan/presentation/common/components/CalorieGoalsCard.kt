package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R
import java.util.Locale

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
    val fillColor = AppTheme.colors.Teal1000
    val moodRatio = if (tdee > 0) caloriesGained.toFloat() / tdee else 0f
    val progress = moodRatio.coerceIn(0f, 1f)
    val moodDrawable = when {
        moodRatio > 1f -> R.drawable.angry
        moodRatio >= 0.5f -> R.drawable.happy
        else -> R.drawable.normal
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.Large)
            .background(AppTheme.colors.Teal500)
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
                modifier = Modifier.size(80.dp),
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
