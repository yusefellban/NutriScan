package iti.grad.nutriscan.domain.steps.history.usecase

import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.nutriscan.domain.steps.history.repository.IStepHistoryRepository
import javax.inject.Inject

class GetStepHistoryUseCase @Inject constructor(
    private val repository: IStepHistoryRepository
) {
    suspend operator fun invoke(period: StepHistoryPeriod): Result<StepHistorySummary> {
        return repository.getStepHistory(period)
    }
}
