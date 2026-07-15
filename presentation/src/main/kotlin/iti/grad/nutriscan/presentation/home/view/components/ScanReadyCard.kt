package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R

/**
 * Dashed-border scan-ready CTA card displayed on the Home screen.
 */
@Composable
fun ScanReadyCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = AppTheme.colors.Teal500

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .customShadow(
                shape = RoundedCornerShape(21.dp),
                color = AppTheme.colors.Teal700.copy(alpha = 0.3f),
                blurRadius = 100f,
                offsetY = 0f
            )
            .background(AppTheme.colors.Surface, RoundedCornerShape(21.dp))
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(21.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(7.5f.dp.toPx(), 5.dp.toPx()),
                            phase = 0f,
                        ),
                    ),
                )
            }
            .clip(RoundedCornerShape(21.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppTheme.colors.Primary),
                onClick = onClick,
            )
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_qr_scan),
            contentDescription = stringResource(R.string.home_ready_to_scan),
            tint = AppTheme.colors.PrimaryVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.home_ready_to_scan),
            style = AppTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.PrimaryVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_scan_subtitle),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.Teal800,
        )
    }
}

