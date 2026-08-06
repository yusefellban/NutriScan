package iti.grad.nutriscan.presentation.main.calories.stephistory.view.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.stepHistoryCardShadow

@Composable
fun StepHistorySummaryRow(
    summary: StepHistorySummary?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Distance",
            value = "${summary?.totalDistanceKm ?: 0.0} km",
            iconRes = R.drawable.ic_map_pin
        )
        
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Calories",
            value = "${summary?.totalCaloriesBurned ?: 0} kcal",
            iconRes = R.drawable.ic_flame
        )
        
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Time",
            value = "${summary?.totalActiveMinutes ?: 0} min",
            iconRes = R.drawable.ic_stopwatch
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .stepHistoryCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.Surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.StepHistorySummaryIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = AppTheme.colors.Teal1200,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppTheme.colors.Teal1200
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.TextPrimary
                )
            )
        }
    }
}

