package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class UpdateWaterCntUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(waterCnt: Int): Result<Unit> = repository.updateWaterCnt(waterCnt)
}
