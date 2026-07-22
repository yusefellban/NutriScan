package iti.grad.nutriscan.presentation.settings.app.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.LexendDeca

/**
 * Generic 2/3-way segmented pill toggle used by the Appearance and Language rows.
 */
@Composable
fun SettingsSegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.colors.AppSettingsToggleContainerBackground)
            .padding(6.dp),
    ) {
        options.forEachIndexed { index, option ->
            SettingsToggleChip(
                label = option,
                isSelected = index == selectedIndex,
                onClick = { onOptionSelected(index) },
            )
        }
    }
}

@Composable
private fun SettingsToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = TextStyle(
            fontFamily = LexendDeca,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            lineHeight = 15.sp,
        ),
        color = if (isSelected) {
            AppTheme.colors.AppSettingsToggleTextSelected
        } else {
            AppTheme.colors.AppSettingsToggleTextUnselected
        },
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSelected) {
                    Modifier.background(AppTheme.colors.AppSettingsToggleChipSelectedBackground)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
