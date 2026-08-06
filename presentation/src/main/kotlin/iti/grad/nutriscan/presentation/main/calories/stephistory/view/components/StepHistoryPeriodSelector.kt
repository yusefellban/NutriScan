package iti.grad.nutriscan.presentation.main.calories.stephistory.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun StepHistoryPeriodSelector(
    selectedPeriod: StepHistoryPeriod,
    onPeriodSelected: (StepHistoryPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods: List<Pair<StepHistoryPeriod, Int>> = listOf(
        StepHistoryPeriod.WEEK to R.string.step_history_period_week,
        StepHistoryPeriod.MONTH to R.string.step_history_period_month,
        StepHistoryPeriod.THREE_MONTHS to R.string.step_history_period_3months,
        StepHistoryPeriod.SIX_MONTHS to R.string.step_history_period_6months
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(AppTheme.colors.StepHistoryChipContainerBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { (period, stringRes) ->
            val isSelected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (isSelected) AppTheme.colors.StepHistoryChipBgSelected
                        else AppTheme.colors.StepHistoryChipBgUnselected
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = stringRes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AppTheme.colors.TextPrimary
                        else AppTheme.colors.StepHistoryChipTextUnselected
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

