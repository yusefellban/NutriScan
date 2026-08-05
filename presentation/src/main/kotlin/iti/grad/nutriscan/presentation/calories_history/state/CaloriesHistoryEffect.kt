package iti.grad.nutriscan.presentation.calories_history.state

sealed interface CaloriesHistoryEffect {
    data object NavigateBack : CaloriesHistoryEffect
    data object NavigateToAddMeals : CaloriesHistoryEffect
}
