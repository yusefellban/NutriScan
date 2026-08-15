package iti.grad.nutriscan.presentation.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import iti.grad.presentation.R
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.isAppRtl
import iti.grad.nutriscan.presentation.common.util.tick
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ProductCard(
    imageUrl: String?,
    productName: String,
    verdict: ProductVerdict?,
    calories: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFailed: Boolean = false,
    swipeAction: ProductCardSwipeAction? = null,
    /** Renders a full-card-width delete strip at the bottom (Calories food log) instead of a
     * swipe gesture. Mutually exclusive with [swipeAction] in practice. */
    onDeleteClick: (() -> Unit)? = null,
    /** Food log (Calories) overlays kcal on the image; the Saved catalog keeps it in the info row. */
    caloriesOverlayOnImage: Boolean = false,
    /** Times this product was logged today. Shows an "x2"-style badge next to the calorie badge
     * instead of duplicating the card; 1 (the default) shows no badge. */
    quantity: Int = 1,
    /** Draws this card's own drop shadow. Calories' food row disables it — that row already sits
     * inside a container that carries one shadow for the whole group (see CaloriesScreen). */
    showShadow: Boolean = true,
) {
    val density = LocalDensity.current
    val shadowBlurPx = with(density) { 12.dp.toPx() }
    val shadowOffsetYPx = with(density) { 6.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showShadow) {
                    Modifier.customShadow(
                        shape = RoundedCornerShape(12.dp),
                        color = AppTheme.colors.ProductCardShadow,
                        blurRadius = shadowBlurPx,
                        offsetX = 0f,
                        offsetY = shadowOffsetYPx
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.ProductCardBackground
        ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                    .height(130.dp)
            ) {
                AdaptiveAsyncImage(
                    model = imageUrl,
                    contentDescription = productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    error = painterResource(id = R.drawable.ic_scanner),
                    placeholder = painterResource(id = R.drawable.ic_scanner)
                )

                if (caloriesOverlayOnImage) {
                    CaloriesBadge(
                        calories = calories,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .zIndex(1f)
                            .padding(6.dp),
                        background = AppTheme.colors.ProductCardNameText.copy(alpha = 0.55f),
                        textColor = Color.White,
                    )

                    if (quantity > 1) {
                        QuantityBadge(
                            quantity = quantity,
                            background = AppTheme.colors.ProductCardNameText.copy(alpha = 0.55f),
                            textColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .zIndex(1f)
                                .padding(6.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (caloriesOverlayOnImage) {
                    Text(
                        text = productName.ifBlank { stringResource(R.string.scan_product_unknown) },
                        style = AppTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = AppTheme.colors.ProductCardNameText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    when {
                        isFailed -> FailedBadge()
                        verdict != null -> VerdictBadge(verdict = verdict)
                    }
                } else {
                    // Original layout: name+verdict on the left, kcal stacked badge on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = productName.ifBlank { stringResource(R.string.scan_product_unknown) },
                                style = AppTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = AppTheme.colors.ProductCardNameText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            when {
                                isFailed -> FailedBadge()
                                verdict != null -> VerdictBadge(verdict = verdict)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (quantity > 1) {
                                QuantityBadge(
                                    quantity = quantity,
                                    background = AppTheme.colors.ProductCardCaloriesBackground,
                                    textColor = AppTheme.colors.ProductCardCaloriesText,
                                )
                            }
                            CaloriesBadge(
                                calories = calories,
                                background = AppTheme.colors.ProductCardCaloriesBackground,
                                textColor = AppTheme.colors.ProductCardCaloriesText,
                                cornerRadius = 4.dp,
                            )
                        }
                    }
                }

                if (swipeAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Swipe slider row — inside the card
                    SwipeActionButton(swipeAction = swipeAction)
                }

                if (onDeleteClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DeleteStrip(onClick = onDeleteClick)
                }
            }
        }
    }
}
}

@Composable
private fun CaloriesBadge(
    calories: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(background, RoundedCornerShape(cornerRadius))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = calories,
            style = AppTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = textColor
        )
        Text(
            text = stringResource(id = R.string.product_card_kcal_unit),
            style = AppTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = textColor
        )
    }
}

@Composable
private fun QuantityBadge(
    quantity: Int,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = R.string.product_card_quantity_badge, quantity),
        style = AppTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        ),
        color = textColor,
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun SwipeActionButton(swipeAction: ProductCardSwipeAction) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isRtl = isAppRtl()
    val buttonColor = AppTheme.colors.Teal1000
    val haptics = LocalHapticFeedback.current

    // Track the drag offset
    val offsetX = remember { Animatable(0f) }

    // We measure the pill width to constrain dragging
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val buttonWidthDp = 44.dp
    val paddingDp = 6.dp

    val buttonWidthPx = with(density) { buttonWidthDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    // Max distance the button can slide (track width minus button width minus side paddings)
    val maxOffsetPx = (trackWidthPx - buttonWidthPx - paddingPx * 2f).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .background(
                color = AppTheme.colors.ProductCardSwipeContainerBackground,
                shape = RoundedCornerShape(50)
            )
            .padding(paddingDp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Hint text — fades as button slides over it
        Text(
            text = stringResource(id = swipeAction.hintResId),
            style = AppTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1,
            color = AppTheme.colors.ProductCardSwipeText,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = buttonWidthDp + 2.dp, end = 2.dp)
        )

        // Teal sliding button
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(width = buttonWidthDp, height = 32.dp)
                .background(buttonColor, RoundedCornerShape(50))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val effectiveDelta = if (isRtl) -delta else delta
                        scope.launch {
                            val newValue = (offsetX.value + effectiveDelta).coerceIn(0f, maxOffsetPx)
                            offsetX.snapTo(newValue)
                        }
                    },
                    onDragStopped = {
                        // If dragged past 70% of the track → trigger action
                        if (maxOffsetPx > 0f && offsetX.value >= maxOffsetPx * 0.7f) {
                            haptics.tick()
                            swipeAction.onTriggered()
                        }
                        // Always spring back to start
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = spring())
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = if (isRtl) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Small always-visible delete icon, bottom-right below the product name. */
@Composable
private fun DeleteStrip(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppTheme.colors.Teal1500)
            .clickable { haptics.tick(); onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_trash),
            contentDescription = stringResource(id = R.string.food_log_delete_item_action),
            tint = AppTheme.colors.Error,
            modifier = Modifier.size(12.dp),
        )
    }
}

// VerdictBadge is now a shared component — see VerdictBadge.kt

