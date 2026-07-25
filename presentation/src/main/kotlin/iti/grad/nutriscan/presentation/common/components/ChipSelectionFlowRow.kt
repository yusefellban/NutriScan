package iti.grad.nutriscan.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.profile_setup.view.components.SelectableChip
import iti.grad.presentation.R

/**
 * Generic loading/error/content chip-selection row, shared by any screen that
 * lets the user pick from a list of id+label items (diseases, allergies,
 * etc.) via [SelectableChip]. Originally duplicated between
 * `HealthProfileContent`'s private `DiseasesSection`/`AllergiesSection` and
 * the Add Family Member sheet — extracted here so both delegate to one
 * implementation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipSelectionFlowRow(
    items: List<T>,
    selectedIds: List<Int>,
    isLoading: Boolean,
    errorMessage: String?,
    idOf: (T) -> Int,
    labelOf: (T) -> String,
    onToggle: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = AppTheme.colors.Primary,
            strokeWidth = 2.dp,
        )

        errorMessage != null -> Column {
            Text(
                text = errorMessage,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.Error,
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.action_retry),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.Primary,
                )
            }
        }

        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                val id = idOf(item)
                SelectableChip(
                    text = labelOf(item),
                    isSelected = selectedIds.contains(id),
                    onClick = { onToggle(id) },
                )
            }
        }
    }
}
