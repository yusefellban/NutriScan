package iti.grad.nutriscan.presentation.main.calories.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.common.model.ProductUiModel

sealed interface CaloriesEvent {
    data object AddFoodClicked : CaloriesEvent
    data object AddExerciseClicked : CaloriesEvent

    /** Adds a new empty cup to the water tracker (increases [iti.grad.nutriscan.presentation.main.calories.state.CaloriesState.waterGoal]). */
    data object AddWaterClicked : CaloriesEvent

    /** Taps the cup at [index] — fills it if it's the next empty cup, unfills it if it's the last filled one, no-op otherwise. */
    data class WaterCupClicked(val index: Int) : CaloriesEvent

    /** Long-presses the cup at [index] to delete it — only responds if it's the last cup. */
    data class WaterCupLongPressed(val index: Int) : CaloriesEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : CaloriesEvent

    /** Checks Health Connect availability/permission — dispatched once on screen start, and again on every steps-gauge tap as a retry. */
    data object StepsCardClicked : CaloriesEvent

    /** Reported back by the screen after the user responds to the Health Connect permission dialog. */
    data class StepsPermissionResult(val granted: Boolean) : CaloriesEvent

    /** Swiped a food-log entry — shows the removal confirmation dialog, does not remove yet. */
    data class FoodItemSwipedToRemove(val entryId: String) : CaloriesEvent
    data object RemoveFoodConfirmed : CaloriesEvent
    data object RemoveFoodDismissed : CaloriesEvent
    data class FoodItemClicked(val product: ProductUiModel) : CaloriesEvent
}
