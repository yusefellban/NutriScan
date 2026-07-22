package iti.grad.nutriscan.domain.foodlog.usecase

import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import javax.inject.Inject

class AddFoodEntryUseCase @Inject constructor(
    private val foodLogRepository: IFoodLogRepository
) {
    suspend operator fun invoke(entry: FoodLogEntry): Result<Unit> = foodLogRepository.addFoodEntry(entry)
}
