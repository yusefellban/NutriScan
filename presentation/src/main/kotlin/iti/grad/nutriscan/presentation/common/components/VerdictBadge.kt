package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Shared verdict pill used across Product Card, Product Details, and any
 * screen that needs to display a SAFE / CAUTION / UNSAFE indicator.
 */
@Composable
fun VerdictBadge(
    verdict: ProductVerdict,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor, textResId) = when (verdict) {
        ProductVerdict.SAFE -> Triple(AppTheme.colors.ProductCardVerdictBackground, AppTheme.colors.ProductCardVerdictText, R.string.verdict_safe)
        ProductVerdict.CAUTION -> Triple(AppTheme.colors.VerdictYellow, AppTheme.colors.ProductCardCautionText, R.string.verdict_caution)
        ProductVerdict.UNSAFE -> Triple(AppTheme.colors.Error, AppTheme.colors.ProductCardVerdictText, R.string.verdict_unsafe)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
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
