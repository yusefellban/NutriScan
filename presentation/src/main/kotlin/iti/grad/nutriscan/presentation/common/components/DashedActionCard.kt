package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Dashed-border "add new item" affordance, e.g. "Add Food" on the Calories Dashboard.
 * Sizes to whatever [modifier] the caller supplies (e.g. `fillMaxWidth()` standalone,
 * or a fixed `width()` alongside other cards in a carousel).
 */
@Composable
fun DashedActionCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 32.dp,
) {
    val isDark = AppTheme.isDark
    val borderColor = if (isDark) AppTheme.colors.CaloriesAccentTeal1200 else AppTheme.colors.Gray600
    val backgroundColor = if (isDark) AppTheme.colors.Teal1400 else AppTheme.colors.Surface
    val iconBackgroundColor = if (isDark) AppTheme.colors.Teal1600 else AppTheme.colors.Teal100
    val iconTint = AppTheme.colors.Teal1000
    val textColor = if (isDark) AppTheme.colors.Teal700 else AppTheme.colors.Teal1400

    Column(
        modifier = modifier
            .clip(AppTheme.shapes.Large)
            .background(backgroundColor)
            .dashedBorder(1.dp, borderColor, 24.dp, dashLength = 6.dp, gapLength = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
