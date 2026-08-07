package iti.grad.nutriscan.domain.foodlog.usecase

import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayFoodLogUseCase @Inject constructor(
    private val foodLogRepository: IFoodLogRepository
) {
    operator fun invoke(): Flow<List<FoodLogEntry>> = foodLogRepository.observeTodayFoodLog()
}
