package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        ProductVerdict.SAFE -> Triple(AppTheme.colors.Teal1000, AppTheme.colors.Teal100, R.string.verdict_safe)
        ProductVerdict.CAUTION -> Triple(AppTheme.colors.VerdictYellow, AppTheme.colors.ProductCardCautionText, R.string.verdict_caution)
        ProductVerdict.UNSAFE -> Triple(AppTheme.colors.Error, AppTheme.colors.Teal100, R.string.verdict_unsafe)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = textResId),
            style = AppTheme.typography.labelLarge,
            color = textColor
        )
    }
}

/**
 * Badge shown when a scan has status = FAILED.
 * Matches the requested style: gray background, white text, no icon.
 */
@Composable
fun FailedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Gray, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(id = R.string.scan_status_failed),
            style = AppTheme.typography.labelLarge,
            color = Color.White
        )
    }
}



