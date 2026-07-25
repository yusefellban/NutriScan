package iti.grad.nutriscan.presentation.common.components
import androidx.compose.ui.graphics.Color

import iti.grad.nutriscan.presentation.common.theme.AppTheme
import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.Modifier
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

@Composable
fun AppSnackbar(
    message: String,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        containerColor = AppTheme.colors.Background,
        contentColor = Color.Black
    ) {
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium
        )
    }
}
