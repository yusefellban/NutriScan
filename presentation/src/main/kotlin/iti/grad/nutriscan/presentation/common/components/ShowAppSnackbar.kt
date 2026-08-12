package iti.grad.nutriscan.presentation.common.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Convenience extension that shows a typed [AppSnackbar] via the host state.
 *
 * Usage:
 * ```
 * snackbarHostState.showAppSnackbar(
 *     message = "Item removed",
 *     type = SnackbarType.SUCCESS
 * )
 * ```
 */
suspend fun SnackbarHostState.showAppSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.SUCCESS,
    title: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult = showSnackbar(
    AppSnackbarVisuals(
        message = message,
        type = type,
        title = title,
        duration = duration
    )
)
