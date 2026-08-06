package iti.grad.nutriscan.presentation.calories_history.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

/**
 * A single stat box inside [CaloriesHistoryDayCard].
 *
 * Layout (matches Figma screenshot):
 *   ┌─────────────────────┐
 *   │ [icon] Label text   │
 *   │ 2400 Kcal           │  ← primaryValue (big) + primaryUnit (small, inline)
 *   │ 8 target            │  ← secondaryValue (medium) + secondaryUnit (small) — optional
 *   └─────────────────────┘
 */
@Composable
fun CaloriesHistoryStatItem(
    icon: Painter,
    label: String,
    primaryValue: String,
    primaryUnit: String,
    secondaryValue: String? = null,
    secondaryUnit: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .customShadow(
                shape = RoundedCornerShape(14.dp),
                color = Color(AppTheme.colors.Teal1000.value).copy(alpha = 0.2f),
                blurRadius = 20f,
                offsetY = 10f,
                spread = (-5).dp,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.Background)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Icon + Label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Teal icon background
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppTheme.colors.CaloriesHistoryTopBarIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = AppTheme.colors.Teal1000,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = AppTheme.typography.labelSmall.copy(
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                ),
                color = AppTheme.colors.CaloriesHistoryStatLabel,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Primary value + unit inline (value large, unit small baseline-aligned)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = primaryValue,
                style = AppTheme.typography.bodyLarge.copy(
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                ),
                color = AppTheme.colors.CaloriesHistoryStatValue,
            )
            Text(
                text = primaryUnit,
                modifier = Modifier.padding(bottom = 3.dp),
                style = AppTheme.typography.labelSmall.copy(
                    fontFamily = LexendDeca,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                ),
                color = AppTheme.colors.CaloriesHistoryStatSecondary,
            )
        }

        // Secondary value + unit (optional)
        if (secondaryValue != null && secondaryUnit != null) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = secondaryValue,
                    style = AppTheme.typography.bodyLarge.copy(
                        fontFamily = LexendDeca,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                    ),
                    color = AppTheme.colors.CaloriesHistoryStatValue,
                )
                Text(
                    text = secondaryUnit,
                    modifier = Modifier.padding(bottom = 2.dp),
                    style = AppTheme.typography.labelSmall.copy(
                        fontFamily = LexendDeca,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                    ),
                    color = AppTheme.colors.CaloriesHistoryStatSecondary,
                )
            }
        }
    }
}


