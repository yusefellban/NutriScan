package iti.grad.nutriscan.presentation.settings.profile.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.model.bmiCategory
import iti.grad.nutriscan.presentation.common.model.pillColor
import iti.grad.nutriscan.presentation.common.model.textColor
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans
import iti.grad.presentation.R

/**
 * Read-only card that displays server-computed BMI and TDEE side-by-side.
 *
 * The card is hidden entirely when both values are null (i.e., the backend
 * has not computed them yet). This avoids showing an empty placeholder on
 * first launch before the profile sync completes.
 *
 * Design:
 * - Two metric tiles in a [Row] inside a card with the profile sheet's background
 * - BMI tile shows the value + a color-coded range pill (Underweight / Normal / Overweight / Obese)
 * - TDEE tile shows the rounded value + "kcal/day" unit label
 * - A subtle lock icon + "Calculated by server · read-only" subtitle communicates non-editability
 */
@Composable
fun BmiTdeeCard(
    bmi: Double?,
    tdee: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.ProfileMenuRowBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_health_news),
                contentDescription = null,
                tint = AppTheme.colors.Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.user_profile_health_metrics_title),
                    style = AppTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = AppTheme.colors.TextPrimary,
                )
                Text(
                    text = stringResource(R.string.user_profile_health_metrics_subtitle),
                    style = AppTheme.typography.bodySmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                    ),
                    color = AppTheme.colors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        // ── Metric Tiles OR Not-Available Placeholder ──────────────────────────
        if (bmi == null && tdee == null) {
            // First launch before any sync — show a clear explanation.
            Text(
                text = stringResource(R.string.bmi_tdee_not_available),
                style = AppTheme.typography.bodySmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                ),
                color = AppTheme.colors.TextSecondary,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (bmi != null) {
                    BmiTile(
                        bmi = bmi,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (tdee != null) {
                    TdeeTile(
                        tdee = tdee,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── BMI tile ────────────────────────────────────────────────────────────────

@Composable
private fun BmiTile(
    bmi: Double,
    modifier: Modifier = Modifier,
) {
    val category = bmiCategory(bmi)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.ProfileSheetBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.user_profile_bmi_label),
            style = AppTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = AppTheme.colors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Animated value
        AnimatedMetricValue(value = "%.1f".format(bmi))

        Spacer(modifier = Modifier.height(8.dp))

        // Color-coded range pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(category.pillColor())
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = stringResource(category.labelRes),
                style = AppTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                ),
                color = category.textColor(),
            )
        }
    }
}

// ── TDEE tile ───────────────────────────────────────────────────────────────

@Composable
private fun TdeeTile(
    tdee: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.ProfileSheetBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.user_profile_tdee_label),
            style = AppTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = AppTheme.colors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Animated value (rounded to nearest integer for display)
        AnimatedMetricValue(value = tdee.toInt().toString())

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.user_profile_tdee_unit),
            style = AppTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            ),
            color = AppTheme.colors.Primary,
        )
    }
}

// ── Animated numeric value ───────────────────────────────────────────────────

@Composable
private fun AnimatedMetricValue(value: String) {
    var triggered by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "metric_fade_in",
    )
    LaunchedEffect(value) { triggered = true }

    Text(
        text = value,
        style = AppTheme.typography.headlineMedium.copy(
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
        ),
        color = AppTheme.colors.TextPrimary.copy(alpha = alpha),
    )
}
