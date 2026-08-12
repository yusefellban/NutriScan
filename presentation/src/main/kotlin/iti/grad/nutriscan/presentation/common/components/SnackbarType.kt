package iti.grad.nutriscan.presentation.common.components

/**
 * Semantic type for app-wide snackbars.
 *
 * Each type maps to a specific leading icon from `res/drawable/`:
 * - [WARNING] → `warning_ic` (yellow triangle)
 * - [SUCCESS] → `success_ic` (teal checkmark)
 * - [ERROR]   → `error_ic`   (red exclamation)
 */
enum class SnackbarType {
    WARNING,
    SUCCESS,
    ERROR
}
