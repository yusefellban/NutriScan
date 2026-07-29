package iti.grad.nutriscan.presentation.settings.terms.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun TermsSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.AppSettingsRowLabel,
        )
        Text(
            text = body,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.AuthDialogSubtitle,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
