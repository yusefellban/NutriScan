package iti.grad.nutriscan.presentation.main.calories.stephistory.state

sealed interface StepHistoryEffect {
    data object NavigateBack : StepHistoryEffect
    data class ShowError(val message: String?) : StepHistoryEffect
}
