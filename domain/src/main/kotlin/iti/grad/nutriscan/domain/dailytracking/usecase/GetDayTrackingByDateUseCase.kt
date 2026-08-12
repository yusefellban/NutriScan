package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import java.time.LocalDate
import javax.inject.Inject

class GetDayTrackingByDateUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<DailyTrackingSummary> =
        repository.getRemoteDaySummary(date)
}
