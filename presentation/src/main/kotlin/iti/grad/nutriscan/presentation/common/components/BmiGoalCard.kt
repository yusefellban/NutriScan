package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.model.bmiCategory
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R

/** BMI range the progress track spans — below [BMI_TRACK_MIN] and above [BMI_TRACK_MAX] both clamp
 * to the track's ends, since a raw BMI can run well outside a chart-worthy 15-35 range. */
private const val BMI_TRACK_MIN = 15.0
private const val BMI_TRACK_MAX = 35.0

/**
 * Same Teal500 card shell as [CalorieGoalsCard] (this app's TDEE/calories-gained
 * card), so the two sit as equal-weight pages in the Calories screen's swipeable
 * pager. Per the Figma spec (node 1764:2096), this card shows only the fire icon,
 * title, and a position-on-range progress bar — no numeric BMI value or category
 * pill on the card face. Shown even when [bmi] is null — the backend hasn't
 * computed it yet — with an explanatory placeholder rather than being hidden, so
 * the pager's page count and dot indicator never change based on data availability.
 */
@Composable
fun BmiGoalCard(
    bmi: Double?,
    modifier: Modifier = Modifier,
) {
    val trackColor = AppTheme.colors.Teal300
    val fillColor = AppTheme.colors.Teal1000
    val progress = if (bmi == null) {
        0f
    } else {
        (((bmi - BMI_TRACK_MIN) / (BMI_TRACK_MAX - BMI_TRACK_MIN)).toFloat()).coerceIn(0f, 1f)
    }
    val contentDescription = bmi?.let { stringResource(bmiCategory(it).labelRes) }

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
                painter = painterResource(R.drawable.ic_fire_solid),
                contentDescription = contentDescription,
                tint = AppTheme.colors.CaloriesIconOnAccent,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.your_bmi),
                style = CaloriesTypography.sectionTitle,
                color = AppTheme.colors.Teal100,
            )
        }

        Spacer(modifier = Modifier.height(46.dp))

        if (bmi == null) {
            Text(
                text = stringResource(R.string.bmi_tdee_not_available),
                style = AppTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = AppTheme.colors.Teal1600,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
        ) {
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
