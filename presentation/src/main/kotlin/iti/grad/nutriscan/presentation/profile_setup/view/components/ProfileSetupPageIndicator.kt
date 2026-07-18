package iti.grad.nutriscan.presentation.profile_setup.view.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = AppTheme.colors.PageIndicatorCurrent)) {
                append("${currentPage + 1}")
            }
            withStyle(SpanStyle(color = AppTheme.colors.PageIndicatorTotal)) {
                append("/$pageCount")
            }
        },
        fontFamily = LexendDeca,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        modifier = modifier
    )
}
