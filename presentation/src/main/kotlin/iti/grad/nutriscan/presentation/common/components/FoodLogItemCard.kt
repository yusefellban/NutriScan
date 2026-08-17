package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.BorderStroke
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

private val CardWidth = 160.dp
private val CardHeight = 116.dp
private val AddCardWidth = 96.dp

/**
 * A single logged meal in the Calories screen's "Daily Products" horizontal-scroll row.
 * 160x116dp footprint, matching iOS `CalorieMealCard` (`DailyProductsSection.swift`).
 * Top row: thumbnail, name (2 lines), quantity badge. Bottom row: calorie badge, minus
 * (-1 serving, only shown when quantity > 1), delete (removes every serving).
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
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.colors.ProductCardBackground,
        border = BorderStroke(1.dp, AppTheme.colors.Gray300),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdaptiveAsyncImage(
                    model = imageUrl,
                    contentDescription = productName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = painterResource(id = R.drawable.ic_photo_placeholder),
                    error = painterResource(id = R.drawable.ic_photo_placeholder),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = productName.ifBlank { stringResource(R.string.scan_product_unknown) },
                        style = AppTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = AppTheme.colors.ProductCardNameText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    QuantityBadge(
                        quantity = quantity,
                        background = AppTheme.colors.Teal200,
                        textColor = AppTheme.colors.Teal1600,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CaloriesBadge(
                    calories = calories,
                    background = AppTheme.colors.ProductCardCaloriesBackground,
                    textColor = AppTheme.colors.ProductCardCaloriesText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (quantity > 1) {
                        FoodLogRoundIconButton(
                            icon = R.drawable.ic_minus,
                            contentDescription = stringResource(R.string.food_log_minus_item_action),
                            background = AppTheme.colors.Gray200,
                            tint = AppTheme.colors.Gray800,
                            onClick = onMinusClick,
                        )
                    }
                    FoodLogRoundIconButton(
                        icon = R.drawable.ic_trash,
                        contentDescription = stringResource(R.string.food_log_delete_item_action),
                        background = AppTheme.colors.ErrorBackground,
                        tint = AppTheme.colors.Error,
                        onClick = onDeleteClick,
                    )
                }
            }
        }
    }
}

/** "Add Food" trigger — always the first item in the row, matches iOS `AddFoodCarouselCard`. */
@Composable
fun CompactAddFoodCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(AddCardWidth).height(CardHeight),
        shape = RoundedCornerShape(18.dp),
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
    tint: Color,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { haptics.tick(); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
    }
}
