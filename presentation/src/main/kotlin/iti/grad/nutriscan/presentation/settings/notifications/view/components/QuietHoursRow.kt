package iti.grad.nutriscan.presentation.settings.notifications.view.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    SettingsActionRow(
        icon = icon,
        label = stringResource(R.string.notification_settings_quiet_hours_label),
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${start.format(formatter)} - ${end.format(formatter)}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.Gray500,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AppTheme.colors.Teal1000,
                    ),
                )
            }
        },
    )
}
