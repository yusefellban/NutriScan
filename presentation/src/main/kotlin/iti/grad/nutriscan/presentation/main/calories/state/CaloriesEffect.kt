package iti.grad.nutriscan.presentation.main.calories.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.presentation.common.model.ProductUiModel

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
    data object NavigateToSaved : CaloriesEffect
    data object NavigateToProfile : CaloriesEffect
    data object NavigateToExercises : CaloriesEffect
    data class ShowSnackbar(@StringRes val messageResId: Int) : CaloriesEffect

    /** Ask the screen to request the ACTIVITY_RECOGNITION runtime permission (needed to read the step counter). */
    data object RequestStepsPermission : CaloriesEffect
    data class NavigateToProductDetail(val product: ProductUiModel) : CaloriesEffect
}
