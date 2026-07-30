package iti.grad.nutriscan.presentation.main.calories.stephistory.state

import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary

data class StepHistoryState(
    val selectedPeriod: StepHistoryPeriod = StepHistoryPeriod.WEEK,
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: StepHistorySummary? = null
)
