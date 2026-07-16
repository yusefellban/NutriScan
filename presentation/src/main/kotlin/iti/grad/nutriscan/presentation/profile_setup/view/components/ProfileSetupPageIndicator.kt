package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

@Composable
fun ProfileSetupPageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "${currentPage + 1}/$pageCount",
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = AppTheme.colors.TextSecondary,
        modifier = modifier
    )
}
