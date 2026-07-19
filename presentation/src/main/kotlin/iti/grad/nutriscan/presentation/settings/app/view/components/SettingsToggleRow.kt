package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsActionRow(
        icon = icon,
        label = label,
        modifier = modifier,
        trailing = {
            SettingsSegmentedToggle(
                options = options,
                selectedIndex = selectedIndex,
                onOptionSelected = onOptionSelected,
            )
        },
    )
}
