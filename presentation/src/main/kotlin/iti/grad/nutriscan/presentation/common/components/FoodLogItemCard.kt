package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.tick
import iti.grad.presentation.R

private val CardWidth = 112.dp
private val CardHeight = 84.dp

/**
 * A single logged meal in the Calories screen's "Daily Products" horizontal-scroll row — same
 * 112x84dp footprint as [iti.grad.nutriscan.presentation.settings.profile.view.components.FamilyMemberCard].
 * Top row: thumbnail, name, quantity badge. Bottom row: calorie pill, minus (-1 serving),
 * delete (removes every serving).
 */
@Composable
fun FoodLogItemCard(
    imageUrl: String?,
    productName: String,
    calories: String,
    quantity: Int,
    onMinusClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(CardWidth)
            .height(CardHeight),
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.colors.FoodLogCardBackground,
    ) {
        Column(
            modifier = Modifier.padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AdaptiveAsyncImage(
                    model = imageUrl,
                    contentDescription = productName,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = painterResource(id = R.drawable.ic_photo_placeholder),
                    error = painterResource(id = R.drawable.ic_photo_placeholder),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = productName.ifBlank { stringResource(R.string.scan_product_unknown) },
                        style = AppTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = AppTheme.colors.ProductCardNameText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (quantity > 1) {
                        QuantityBadge(
                            quantity = quantity,
                            background = AppTheme.colors.Teal300,
                            textColor = AppTheme.colors.Teal700,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CaloriesBadge(
                    calories = calories,
                    background = AppTheme.colors.Teal300,
                    textColor = AppTheme.colors.Teal700,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    FoodLogRoundIconButton(
                        icon = R.drawable.ic_minus,
                        contentDescription = stringResource(R.string.food_log_minus_item_action),
                        background = AppTheme.colors.Warning,
                        onClick = onMinusClick,
                    )
                    FoodLogRoundIconButton(
                        icon = R.drawable.ic_trash,
                        contentDescription = stringResource(R.string.food_log_delete_item_action),
                        background = AppTheme.colors.Error,
                        onClick = onDeleteClick,
                    )
                }
            }
        }
    }
}

/** "Add Food" trigger sized to match [FoodLogItemCard] exactly — always the first item in the row. */
@Composable
fun CompactAddFoodCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(CardWidth).height(CardHeight),
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.colors.FoodLogCardBackground,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Teal300),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    tint = AppTheme.colors.Teal700,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = stringResource(R.string.add_food),
                style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.ProductCardNameText,
            )
        }
    }
}

@Composable
private fun FoodLogRoundIconButton(
    icon: Int,
    contentDescription: String,
    background: Color,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
    }
}
