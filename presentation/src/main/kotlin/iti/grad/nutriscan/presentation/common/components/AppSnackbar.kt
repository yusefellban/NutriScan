package iti.grad.nutriscan.presentation.common.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@Composable
fun AppSnackbar(
    message: String,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        containerColor = AppTheme.colors.Background,
        contentColor = androidx.compose.ui.graphics.Color.Black
    ) {
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium
        )
    }
}
