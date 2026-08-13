package iti.grad.nutriscan.presentation.calories_history.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryDayUiModel
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

/**
 * Full-width card for one day's calorie summary.
 *
 * Visual structure (matches screenshot):
 *
 *         ╭──────────────────╮
 *         │   23-7-2026      │   ← white date chip, centered
 *         ╰──────────────────╯
 *  ╭─────────────────────────────────────────────╮
 *  │  [TotalMeals] [Water]  [Steps]  [Exercise]  │  ← outer card (light teal bg)
 *  ╰─────────────────────────────────────────────╯
 */
@Composable
fun CaloriesHistoryDayCard(
    entry: CaloriesHistoryDayUiModel,
    modifier: Modifier = Modifier,
) {
    val kcalUnit = stringResource(R.string.calories_history_kcal)
    val cupsUnit = stringResource(R.string.calories_history_cups)
    val targetUnit = stringResource(R.string.calories_history_target)
    val stepUnit = stringResource(R.string.calories_history_step_unit)
    val minUnit = stringResource(R.string.calories_history_min)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Outer card (light teal-gray background)
        Row(
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.CaloriesHistoryOuterCardBg)
                .horizontalScroll(rememberScrollState())
                .padding(top = 28.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val itemModifier = Modifier.height(105.dp)

            // 1. Total Meals

            CaloriesHistoryStatItem(
                modifier = itemModifier,
                icon = painterResource(R.drawable.ic_flame_apple),
                label = stringResource(R.string.calories_history_total_meals),
                primaryValue = "${entry.totalMealsKcal}",
                primaryUnit = kcalUnit,
            )

            // 2. Water
            CaloriesHistoryStatItem(
                modifier = itemModifier,
                icon = painterResource(R.drawable.ic_water_outlined),
                label = stringResource(R.string.calories_history_water),
                primaryValue = "${entry.waterCups}",
                primaryUnit = cupsUnit,
                secondaryValue = "${entry.waterTarget}",
                secondaryUnit = targetUnit,
            )

            // 3. Steps
            CaloriesHistoryStatItem(
                modifier = itemModifier,
                icon = painterResource(R.drawable.steps),
                label = stringResource(R.string.calories_history_steps),
                primaryValue = "${entry.steps}",
                primaryUnit = stepUnit,
                secondaryValue = "${entry.stepsKcal}",
                secondaryUnit = kcalUnit,
            )

            // 4. Exercise
            CaloriesHistoryStatItem(
                modifier = itemModifier,
                icon = painterResource(R.drawable.ic_dumbell),
                label = stringResource(R.string.calories_history_exercise),
                primaryValue = "${entry.exerciseMinutes}",
                primaryUnit = minUnit,
                secondaryValue = "${entry.exerciseKcal}",
                secondaryUnit = kcalUnit,
            )
        }

        // Date chip — white pill overlapping the top edge of the card
        Box(
            modifier = Modifier
                .zIndex(1f)
                .customShadow(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF13A4AB).copy(alpha = 0.2f),
                    blurRadius = 20f,
                    offsetY = 10f,
                    spread = (-5).dp,
                )
                .clip(RoundedCornerShape(50.dp))
                .background(AppTheme.colors.CaloriesHistoryDateChipBg)
                .padding(horizontal = 28.dp, vertical = 8.dp),
        ) {
            Text(
                text = entry.dateLabel,
                style = AppTheme.typography.bodyMedium.copy(
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = AppTheme.colors.CaloriesHistoryStatSecondary,
            )
        }
    }
}
