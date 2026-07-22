package iti.grad.nutriscan.presentation.main.calories.state

import androidx.annotation.StringRes

/**
 * One-shot side effects emitted by the Calories Dashboard ViewModel.
 *
 * Collected in the Composable and forwarded to navController — the ViewModel
 * never holds a reference to NavController (per AGENTS.md §5.3).
 */
sealed interface CaloriesEffect {
    data object NavigateToSavedProducts : CaloriesEffect
    data object NavigateToHome : CaloriesEffect
    data object NavigateToScan : CaloriesEffect
    data object NavigateToShopping : CaloriesEffect
    data object NavigateToProfile : CaloriesEffect
    data class ShowSnackbar(@StringRes val messageResId: Int) : CaloriesEffect
}
