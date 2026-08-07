package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun LogoutButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** When true, renders with Error color + Delete icon for the Delete Account action. */
    isDestructive: Boolean = false,
) {
    val accentColor = if (isDestructive) AppTheme.colors.Error else AppTheme.colors.AppSettingsLogoutAccent
    val iconBgColor = if (isDestructive) AppTheme.colors.ErrorBackground else AppTheme.colors.AppSettingsLogoutIconBackground
    val icon = if (isDestructive) Icons.Default.DeleteForever else Icons.AutoMirrored.Filled.Logout

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(61.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = accentColor, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(iconBgColor.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = accentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
    }
}
