package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * A single entry in the Calories Dashboard's "Add Food" carousel — sits beside
 * [DashedActionCard] and scrolls with it in a [androidx.compose.foundation.lazy.LazyRow].
 */
@Composable
fun FoodEntryCard(
    name: String,
    kcal: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = AppTheme.isDark
    val backgroundColor = if (isDark) AppTheme.colors.Teal1400 else AppTheme.colors.Surface
    val iconBackgroundColor = if (isDark) AppTheme.colors.Teal1600 else AppTheme.colors.Teal100
    val iconTint = AppTheme.colors.Teal1000
    val nameColor = if (isDark) AppTheme.colors.Teal700 else AppTheme.colors.Teal1400

    Column(
        modifier = modifier
            .clip(AppTheme.shapes.Large)
            .background(backgroundColor)
            .padding(16.dp),
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
                imageVector = Icons.Filled.Fastfood,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = name,
            style = AppTheme.typography.bodyMedium,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "$kcal ${stringResource(R.string.kcal_suffix)}",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.ExerciseSecondaryText,
        )
    }
}
