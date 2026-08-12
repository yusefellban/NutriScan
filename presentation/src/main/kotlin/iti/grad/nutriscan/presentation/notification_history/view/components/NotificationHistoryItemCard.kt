package iti.grad.nutriscan.presentation.notification_history.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.notification_history.state.NotificationHistoryItemUi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import iti.grad.nutriscan.domain.notification.model.NotificationType

@Composable
fun NotificationHistoryItemCard(
    item: NotificationHistoryItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (typeIcon, iconTint, iconBackground) = resolveNotificationTypeVisuals(item.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.isRead) AppTheme.colors.Surface else AppTheme.colors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.TextPrimary,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = item.relativeTime.asString(),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.TextSecondary,
                )
            }
            
            Text(
                text = item.body,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun resolveNotificationTypeVisuals(type: NotificationType): Triple<ImageVector, Color, Color> {
    return when (type) {
        NotificationType.STEPS -> Triple(Icons.AutoMirrored.Rounded.DirectionsWalk, AppTheme.colors.Primary, AppTheme.colors.Teal100)
        NotificationType.WATER -> Triple(Icons.Rounded.WaterDrop, AppTheme.colors.PrimaryVariant, AppTheme.colors.Teal200)
        NotificationType.WORKOUT -> Triple(Icons.Rounded.FitnessCenter, AppTheme.colors.Accent, AppTheme.colors.Teal300)
        NotificationType.FOOD -> Triple(Icons.Rounded.Restaurant, AppTheme.colors.VerdictRed, AppTheme.colors.VerdictRedBackground)
        NotificationType.NEWS -> Triple(Icons.AutoMirrored.Rounded.Article, AppTheme.colors.Teal1000, AppTheme.colors.Teal100)
        NotificationType.QUOTE -> Triple(Icons.Rounded.FormatQuote, AppTheme.colors.Teal800, AppTheme.colors.Teal200)
        NotificationType.SCAN -> Triple(Icons.Rounded.QrCodeScanner, AppTheme.colors.Teal1200, AppTheme.colors.Teal300)
        NotificationType.STREAK -> Triple(Icons.Rounded.LocalFireDepartment, AppTheme.colors.Warning, AppTheme.colors.VerdictYellow.copy(alpha = 0.12f))
        NotificationType.BREAK -> Triple(Icons.Rounded.Coffee, AppTheme.colors.Teal1600, AppTheme.colors.Teal100)
    }
}
