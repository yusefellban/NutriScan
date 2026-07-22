package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.presentation.R

@Composable
fun ActiveScanCard(
    scan: ActiveScanUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .customShadow(
                shape = cardShape,
                color = AppTheme.colors.Primary.copy(alpha = 0.35f),
                blurRadius = 50f,
                offsetY = 8f,
            )
            .clip(cardShape)
            .background(AppTheme.colors.PrimaryVariant)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScanThumbnail(thumbnailUrl = scan.thumbnailUrl)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scan.brand ?: stringResource(R.string.scan_brand_unknown),
                style = AppTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.Teal800,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = scan.productName ?: stringResource(R.string.scan_product_unknown),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.OnPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (scan.statusResId != null) {
                ProcessingBadge(statusResId = scan.statusResId)
            } else if (scan.healthTag != null) {
                HealthBadge(text = scan.healthTag)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppTheme.colors.Teal800)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.scan_add_to_list_content_description),
                tint = AppTheme.colors.PrimaryVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ScanThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    if (thumbnailUrl != null) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .background(AppTheme.colors.Divider),
        )
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .background(AppTheme.colors.Divider),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_qr_scan),
                contentDescription = null,
                tint = AppTheme.colors.OnPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ProcessingBadge(
    statusResId: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.VerdictYellow)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(statusResId),
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.TextPrimary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun HealthBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.Teal800.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.Teal800,
            fontSize = 11.sp,
        )
    }
}
