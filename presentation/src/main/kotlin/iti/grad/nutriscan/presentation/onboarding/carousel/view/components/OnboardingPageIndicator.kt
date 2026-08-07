package iti.grad.nutriscan.presentation.onboarding.carousel.view.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingPageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until pageCount) {
            val isSelected = i == currentPage
            val width = animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                label = "indicator_width"
            )
            val colors = AppTheme.colors
            val color = if (isSelected) {
                colors.Primary
            } else {
                colors.TextSecondary.copy(alpha = 0.5f)
            }
            Box(
                modifier = Modifier
                    .width(width.value)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
