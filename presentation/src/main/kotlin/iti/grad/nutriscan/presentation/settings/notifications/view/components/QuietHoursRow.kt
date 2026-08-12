package iti.grad.nutriscan.presentation.settings.notifications.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.app.view.components.SettingsActionRow
import iti.grad.presentation.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun QuietHoursRow(
    icon: Painter,
    start: LocalTime,
    end: LocalTime,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(modifier = modifier) {
        SettingsActionRow(
            icon = icon,
            label = stringResource(R.string.notification_settings_quiet_hours_label),
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AppTheme.colors.Teal1000,
                    ),
                )
            },
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp, start = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(AppTheme.colors.MenuIconContainerBackground.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRangeClick,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${start.format(formatter)} - ${end.format(formatter)}",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.Teal1000,
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppTheme.colors.Teal1000,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

