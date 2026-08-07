package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca
import iti.grad.nutriscan.presentation.common.components.customShadow

@Composable
fun HeightCard(
    height: Int,
    pageOffset: Float,
    isSelected: Boolean,
    cardWidth: Dp,
    modifier: Modifier = Modifier
) {
    val scale = 1f - (pageOffset * 0.25f).coerceIn(0f, 0.25f)
    val alpha = 1f - (pageOffset * 0.6f).coerceIn(0f, 0.6f)

    val cardBg = if (isSelected) AppTheme.colors.Teal500 else AppTheme.colors.HeightUnselectedCardBackground
    val textCol = if (isSelected) AppTheme.colors.HeightSelectedCardText else AppTheme.colors.HeightUnselectedCardText

    val shadowColor = if (isSelected) AppTheme.colors.ShadowSelected else AppTheme.colors.ShadowUnselected
    val isDark = AppTheme.isDark
    val shadowBlur = if (isDark) {
        if (isSelected) 20f else 8f
    } else {
        if (isSelected) 80f else 32f
    }

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .customShadow(
                shape = RoundedCornerShape(24.dp),
                color = shadowColor,
                blurRadius = shadowBlur,
                offsetY = 0f
            )
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = height.toString(),
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = textCol
        )
    }
}

