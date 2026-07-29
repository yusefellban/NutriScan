package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.directionalDrawable
import iti.grad.presentation.R

/**
 * Shared row shell for App Settings entries: leading icon in a soft circle,
 * a label, and a trailing slot (defaults to a chevron for navigable rows).
 */
@Composable
fun SettingsActionRow(
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = { DefaultChevron() },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(61.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.AppSettingsCardBackground)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.AppSettingsIconContainerBackground.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = AppTheme.colors.Teal1000,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = label,
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.AppSettingsRowLabel,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )

        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            trailing()
        }
    }
}

@Composable
private fun DefaultChevron() {
    Icon(
        painter = painterResource(directionalDrawable(R.drawable.ic_arrow_right, R.drawable.ic_arrow_left)),
        contentDescription = null,
        tint = AppTheme.colors.Gray500,
        modifier = Modifier.size(24.dp),
    )
}
