package iti.grad.nutriscan.presentation.main.calories.stephistory.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.stepHistoryCardShadow
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StepHistoryDateRangeRow(
    summary: StepHistorySummary?,
    modifier: Modifier = Modifier
) {
    if (summary == null) return

    val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
    val startStr = summary.startDate.format(formatter)
    val endStr = summary.endDate.format(formatter)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateCard(
            title = stringResource(R.string.step_history_start_date),
            dateStr = startStr,
            modifier = Modifier.weight(1f)
        )
        DateCard(
            title = stringResource(R.string.step_history_end_date),
            dateStr = endStr,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateCard(
    title: String,
    dateStr: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .stepHistoryCardShadow(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.Surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar_small),
                contentDescription = null,
                tint = AppTheme.colors.Teal1200,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = AppTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = AppTheme.colors.Teal1200
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dateStr,
            style = AppTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = AppTheme.colors.TextPrimary
        )
    }
}

