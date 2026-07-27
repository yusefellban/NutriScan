package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import java.time.LocalDate
import javax.inject.Inject

class SyncPendingDailyTrackingUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<Unit> = repository.syncPendingDay(date)
}
