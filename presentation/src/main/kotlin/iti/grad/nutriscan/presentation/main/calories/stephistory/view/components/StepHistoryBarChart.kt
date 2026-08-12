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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.stepHistoryCardShadow
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StepHistoryBarChart(
    summary: StepHistorySummary?,
    modifier: Modifier = Modifier
) {
    val chartData = summary?.monthlyData ?: emptyList()
    val maxSteps = chartData.maxOfOrNull { it.totalSteps }?.takeIf { it > 0 } ?: 1
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .stepHistoryCardShadow(RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.Surface)
            .padding(24.dp)
    ) {
        Text(
            text = "Step History",
            style = AppTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = AppTheme.colors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            chartData.forEach { data ->
                val fillRatio = (data.totalSteps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
                val formattedSteps = numberFormat.format(data.totalSteps)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Number above the bar
                    if (data.totalSteps > 0) {
                        Text(
                            text = formattedSteps,
                            style = AppTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AppTheme.colors.TextPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(18.dp)) // Space for empty bars to align correctly
                    }

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
                        style = AppTheme.typography.labelSmall.copy(
                            color = AppTheme.colors.TextSecondary
                        )
                    )
                }
            }
        }
    }
}

