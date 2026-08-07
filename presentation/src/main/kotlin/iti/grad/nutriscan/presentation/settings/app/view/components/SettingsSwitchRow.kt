package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun SettingsSwitchRow(
    icon: Painter,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsActionRow(
        icon = icon,
        label = label,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AppTheme.colors.Teal1000,
                ),
            )
        },
    )
}
