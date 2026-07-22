package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Exercise summary card with a quick-add affordance for logging a workout.
 */
@Composable
fun ExerciseCard(
    exerciseKcal: Int,
    exerciseMinutes: Int,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) AppTheme.colors.Teal1400 else AppTheme.colors.Surface
    val addButtonBackground = if (isDark) AppTheme.colors.Teal1600 else AppTheme.colors.Gray200
    val addButtonIconTint = if (isDark) AppTheme.colors.Teal1000 else AppTheme.colors.Gray1000
    val bigNumberColor = if (isDark) AppTheme.colors.Teal400 else AppTheme.colors.Gray1600
    val shadowColor = if (isDark) AppTheme.colors.Teal700.copy(alpha = 0.35f) else AppTheme.colors.Teal1000.copy(alpha = 0.2f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(
                shape = RoundedCornerShape(24.dp),
                color = shadowColor,
                blurRadius = 45f,
                offsetY = 15f,
                spread = 5.dp,
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // dumbell.xml already bakes in the exact Teal200 container +
            // Teal1000 glyph colors, unaffected by theme — no tinting needed.
            Icon(
                painter = painterResource(R.drawable.dumbell),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(R.string.your_exercise),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.Teal1000,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(addButtonBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.your_exercise),
                    tint = addButtonIconTint,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = exerciseKcal.toString(),
                style = AppTheme.typography.titleLarge,
                color = bigNumberColor,
            )
            Text(
                text = " ${stringResource(R.string.kcal_exercise)}",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.ExerciseSecondaryText,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = "$exerciseMinutes ${stringResource(R.string.min_today)}",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.ExerciseSecondaryText,
        )
    }
}
