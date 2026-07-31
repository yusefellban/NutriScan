package iti.grad.nutriscan.presentation.common.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

/**
 * Custom [SnackbarVisuals] that carries a [SnackbarType] alongside the message.
 *
 * [AppSnackbar] inspects these visuals to render the correct icon, title text,
 * and color scheme per the Figma spec (Warning / Success / Error variants).
 *
 * @param message     The body text displayed below the title.
 * @param type        Semantic type — drives icon and title defaults.
 * @param title       Optional explicit title. When `null`, defaults to the type name.
 * @param actionLabel Optional action button label (unused in current design).
 * @param duration    How long the snackbar stays visible.
 */
data class AppSnackbarVisuals(
    override val message: String,
    val type: SnackbarType = SnackbarType.SUCCESS,
    val title: String? = null,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals
