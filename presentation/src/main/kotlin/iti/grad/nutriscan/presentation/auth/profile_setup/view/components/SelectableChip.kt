package iti.grad.nutriscan.presentation.auth.profile_setup.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
        if (isDark) Color(0xFF13A4AB) else Color(0xFFC0C0C0)
    } else {
        if (isDark) Color(0xFF0B5F65) else Color(0xFFFFFFFF)
    }
    
    val borderColor = if (isSelected) {
        if (isDark) Color(0xFF75DEE3) else Color(0xFF898989)
    } else {
        if (isDark) Color(0xFF108188) else Color(0xFFC0C0C0)
    }
    
    val textColor = if (isSelected) {
        if (isDark) Color(0xFF0F474A) else Color(0xFF3E3E3E)
    } else {
        if (isDark) Color(0xFF2FC5CC) else Color(0xFF3E3E3E)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
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
