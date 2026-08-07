package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingHistoryPage
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class GetCaloriesHistoryUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(page: Int, size: Int): Result<DailyTrackingHistoryPage> =
        repository.getHistoryPage(page, size)
}
