package iti.grad.nutriscan.presentation.onboarding.profile_setup.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.PlusJakartaSans

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val backgroundColor = if (isSelected) {
        if (isDark) AppTheme.colors.PrimaryVariant else AppTheme.colors.Primary
    } else {
        AppTheme.colors.Surface
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        AppTheme.colors.TextPrimary
    }
    
    val borderModifier = if (isSelected) {
        Modifier
    } else {
        Modifier.border(
            width = 1.dp,
            color = AppTheme.colors.Divider,
            shape = RoundedCornerShape(10.dp)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}
