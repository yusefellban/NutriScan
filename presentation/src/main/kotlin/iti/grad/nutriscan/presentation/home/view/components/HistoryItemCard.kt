package iti.grad.nutriscan.presentation.home.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.components.customShadow
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.state.HomeHistoryItem
import iti.grad.nutriscan.presentation.home.state.VerdictType
import iti.grad.presentation.R

/**
 * A single card in the Recent History list showing product name, scan date,
 * and a color-coded verdict badge.
 */
@Composable
fun HistoryItemCard(
    item: HomeHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .customShadow(
                shape = RoundedCornerShape(22.dp),
                color = AppTheme.colors.Primary.copy(alpha = 0.2f),
                blurRadius = 60f,
                offsetY = 0f
            )
            .clip(RoundedCornerShape(22.dp))
            .background(AppTheme.colors.Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Product thumbnail
        if (item.imageUrl != null) {
            coil3.compose.AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Divider)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.Divider),
                contentAlignment = Alignment.Center,
            ) {
                // Empty placeholder
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Product name + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.HistoryItemTitleColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.scanDate,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.HistoryItemDateColor,
            )
        }

        // Verdict badge
        VerdictBadge(item = item)
    }
}

@Composable
private fun VerdictBadge(
    item: HomeHistoryItem,
    modifier: Modifier = Modifier,
) {
    val (badgeColor, iconResId) = when (item.verdictType) {
        VerdictType.GREEN -> AppTheme.colors.VerdictGreenBadgeColor to R.drawable.ic_verified
        VerdictType.CYAN -> AppTheme.colors.VerdictCyanBadgeColor to R.drawable.ic_verified
        VerdictType.YELLOW -> AppTheme.colors.VerdictYellow to R.drawable.ic_solid_warning
        VerdictType.RED -> AppTheme.colors.VerdictRed to R.drawable.ic_solid_warning
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            tint = if (item.verdictType == VerdictType.RED) AppTheme.colors.VerdictRedWarningIconTint else badgeColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (item.verdictType == VerdictType.RED) AppTheme.colors.VerdictRedBackground 
                    else badgeColor.copy(alpha = 0.15f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(item.verdictLabelResId),
                style = AppTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.verdictType == VerdictType.RED) AppTheme.colors.VerdictRedText else badgeColor,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
