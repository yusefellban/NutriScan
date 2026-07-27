package iti.grad.nutriscan.presentation.common.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.model.bmiCategory
import iti.grad.nutriscan.presentation.common.model.pillColor
import iti.grad.nutriscan.presentation.common.model.textColor
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R

/**
 * Same Teal500 card shell as [CalorieGoalsCard] (this app's TDEE/calories-gained
 * card), so the two sit as equal-weight pages in the Calories screen's swipeable
 * pager. Shown even when [bmi] is null — the backend hasn't computed it yet —
 * with an explanatory placeholder rather than being hidden, so the pager's page
 * count and dot indicator never change based on data availability.
 */
@Composable
fun BmiGoalCard(
    bmi: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.Large)
            .background(AppTheme.colors.Teal500)
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_health_news),
                contentDescription = null,
                tint = AppTheme.colors.CaloriesIconOnAccent,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.your_bmi),
                style = CaloriesTypography.sectionTitle,
                color = AppTheme.colors.Teal100,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (bmi == null) {
            Text(
                text = stringResource(R.string.bmi_tdee_not_available),
                style = AppTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = AppTheme.colors.Teal1600,
            )
            Spacer(modifier = Modifier.height(56.dp))
        } else {
            val category = bmiCategory(bmi)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "%.1f".format(bmi),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.Teal100,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(category.pillColor())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(category.labelRes),
                        style = AppTheme.typography.labelSmall,
                        color = category.textColor(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
