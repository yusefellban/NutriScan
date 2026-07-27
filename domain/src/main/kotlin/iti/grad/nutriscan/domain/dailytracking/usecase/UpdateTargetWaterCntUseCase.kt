package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateTargetWaterCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(targetWaterCnt: Int): Result<Unit> = repository.updateTargetWaterCnt(targetWaterCnt)
}
