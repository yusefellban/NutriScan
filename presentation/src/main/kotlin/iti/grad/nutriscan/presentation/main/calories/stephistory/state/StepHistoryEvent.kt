package iti.grad.nutriscan.presentation.main.calories.stephistory.state

import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod

sealed interface StepHistoryEvent {
    data class SelectPeriod(val period: StepHistoryPeriod) : StepHistoryEvent
    data object Retry : StepHistoryEvent
    data object NavigateBack : StepHistoryEvent
}
