package iti.grad.nutriscan.presentation.common.components

import androidx.annotation.StringRes
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.model.ProductVerdict
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ProductCard(
    imageUrl: String?,
    productName: String,
    verdict: ProductVerdict,
    calories: String,
    @StringRes swipeHintResId: Int,
    onClick: () -> Unit,
    onSwipeToAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val shadowBlurPx = with(density) { 30.dp.toPx() }
    val shadowOffsetYPx = with(density) { 15.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.ProductCardShadow,
                blurRadius = shadowBlurPx,
                offsetX = 0f,
                offsetY = shadowOffsetYPx
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
            // Product image — inside the card with padding
            AsyncImage(
                model = imageUrl,
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_scanner),
                placeholder = painterResource(id = R.drawable.ic_scanner)
            )

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Row containing Title+Verdict and Kcal Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = productName,
                            style = AppTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = AppTheme.colors.ProductCardNameText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Verdict badge
                        VerdictBadge(verdict = verdict)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Kcal stacked badge
                    Box(
                        modifier = Modifier
                            .background(AppTheme.colors.ProductCardCaloriesBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = calories,
                                style = AppTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = AppTheme.colors.ProductCardCaloriesText
                            )
                            Text(
                                text = stringResource(id = R.string.product_card_kcal_unit),
                                style = AppTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = AppTheme.colors.ProductCardCaloriesText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Swipe slider row — inside the card
                SwipeToAddButton(
                    swipeHintResId = swipeHintResId,
                    onSwipeToAdd = onSwipeToAdd
                )
            }
        }
    }
}
}

@Composable
private fun SwipeToAddButton(
    @StringRes swipeHintResId: Int,
    onSwipeToAdd: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

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
            text = stringResource(id = swipeHintResId),
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
                .background(AppTheme.colors.ProductCardSwipeIconBackground, RoundedCornerShape(50))
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
                            onSwipeToAdd()
                        }
                        // Always spring back to start
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = spring())
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = if (isRtl) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun VerdictBadge(verdict: ProductVerdict) {
    val (backgroundColor, textColor, textResId) = when (verdict) {
        ProductVerdict.SAFE -> Triple(AppTheme.colors.ProductCardVerdictBackground, AppTheme.colors.ProductCardVerdictText, R.string.verdict_safe)
        ProductVerdict.CAUTION -> Triple(AppTheme.colors.VerdictYellow, AppTheme.colors.ProductCardCautionText, R.string.verdict_caution)
        ProductVerdict.UNSAFE -> Triple(AppTheme.colors.Error, AppTheme.colors.ProductCardVerdictText, R.string.verdict_unsafe)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = textResId),
            style = AppTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = textColor
        )
    }
}
