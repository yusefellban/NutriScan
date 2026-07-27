package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayDailyTrackingUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    operator fun invoke(): Flow<DailyTracking> = repository.observeToday()
}
