package iti.grad.nutriscan.presentation.main.calories.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

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
}
