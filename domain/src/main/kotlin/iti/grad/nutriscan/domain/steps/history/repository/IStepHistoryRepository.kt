package iti.grad.nutriscan.domain.steps.history.repository

import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary

interface IStepHistoryRepository {
    suspend fun getStepHistory(period: StepHistoryPeriod): Result<StepHistorySummary>
}
