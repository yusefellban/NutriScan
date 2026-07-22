package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.presentation.R

/**
 * "Water"/count header above a shadowed card of glass icons. The trailing "+"
 * adds a new empty cup; tapping a cup fills/unfills it in order — only the
 * next empty cup or the last filled cup responds, enforced by the ViewModel.
 * Long-pressing a cup deletes it (only the last cup responds, same ordering rule).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaterTrackerCard(
    waterConsumed: Int,
    waterGoal: Int,
    onAddWater: () -> Unit,
    onCupClicked: (Int) -> Unit,
    onCupLongPressed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) AppTheme.colors.Teal300 else AppTheme.colors.Gray1600
    val filledGlassTint = AppTheme.colors.Teal700
    val emptyGlassTint = if (isDark) AppTheme.colors.Teal1300 else AppTheme.colors.Gray400
    val addButtonBackground = if (isDark) AppTheme.colors.Teal1600 else AppTheme.colors.Gray200
    val addButtonIconTint = if (isDark) AppTheme.colors.Teal1000 else AppTheme.colors.Gray1000

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.water),
                style = CaloriesTypography.sectionTitle,
                color = headerColor,
            )
            Text(
                text = "$waterConsumed/$waterGoal",
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.Teal1000,
            )
        }

        Row(
            modifier = Modifier.calorieCardSurface(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(waterGoal) { index ->
                    val isFilled = index < waterConsumed
                    Icon(
                        painter = painterResource(if (isFilled) R.drawable.cup_filled else R.drawable.cup_empty),
                        contentDescription = null,
                        tint = if (isFilled) filledGlassTint else emptyGlassTint,
                        modifier = Modifier
                            .width(20.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onCupClicked(index) },
                                onLongClick = { onCupLongPressed(index) },
                            ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(addButtonBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddWater,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.water),
                    tint = addButtonIconTint,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
