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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.stepHistoryCardShadow
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StepHistoryGaugeCard(
    summary: StepHistorySummary?,
    modifier: Modifier = Modifier
) {
    val averageSteps = summary?.periodAverage ?: 0
    val goal = summary?.stepGoal ?: 10000
    val progress = if (goal > 0) (averageSteps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f

    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedAverage = numberFormat.format(averageSteps)
    val formattedGoal = numberFormat.format(goal)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .stepHistoryCardShadow(RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.StepHistoryGaugeCardBg)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.daily_insight_title),
            style = AppTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = AppTheme.colors.StepHistoryGaugeValueText
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left side: Gauge + Goal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(60.dp),
                        color = AppTheme.colors.StepHistoryGaugeTrack,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(60.dp),
                        color = AppTheme.colors.StepHistoryGaugeFill,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_walking_person),
                        contentDescription = null,
                        tint = AppTheme.colors.StepHistoryGaugeIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedAverage,
                            style = AppTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = AppTheme.colors.StepHistoryGaugeIconTint
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.step_history_steps),
                            style = AppTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = AppTheme.colors.StepHistoryGaugeLabelText,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "of $formattedGoal Goal",
                        style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppTheme.colors.StepHistoryGaugeLabelText
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: Period Average Box
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.StepHistoryChartBarBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.step_history_daily_average),
                    style = AppTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = AppTheme.colors.StepHistoryGaugeLabelText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formattedAverage,
                        style = AppTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = AppTheme.colors.StepHistoryGaugeValueText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.step_history_steps),
                        style = AppTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = AppTheme.colors.StepHistoryGaugeLabelText,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}
