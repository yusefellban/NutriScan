package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun ProductShimmerCard(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val shadowBlurPx = with(density) { 12.dp.toPx() }
    val shadowOffsetYPx = with(density) { 6.dp.toPx() }

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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.ProductCardBackground
        ) {
            Column {
                // Image Shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )

                // Info Section
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Product Name Shimmer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Verdict Badge Shimmer
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Calories Badge Shimmer
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Swipe Action Shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}
