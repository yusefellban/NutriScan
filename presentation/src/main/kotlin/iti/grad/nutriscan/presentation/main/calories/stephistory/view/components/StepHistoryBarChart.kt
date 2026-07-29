package iti.grad.nutriscan.presentation.main.calories.stephistory.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun StepHistoryBarChart(
    summary: StepHistorySummary?,
    modifier: Modifier = Modifier
) {
    val chartData = summary?.monthlyData ?: emptyList()
    val maxSteps = chartData.maxOfOrNull { it.totalSteps }?.takeIf { it > 0 } ?: 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        chartData.forEach { data ->
            val fillRatio = (data.totalSteps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.colors.StepHistoryChartBarBg),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fillRatio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.StepHistoryChartBarFill)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = data.monthLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppTheme.colors.StepHistoryChartLabel
                    )
                )
            }
        }
    }
}
