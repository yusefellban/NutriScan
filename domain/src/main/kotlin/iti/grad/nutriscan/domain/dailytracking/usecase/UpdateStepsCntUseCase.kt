package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateStepsCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(stepsCnt: Int): Result<Unit> = repository.updateStepsCnt(stepsCnt)
}
