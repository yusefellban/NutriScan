package iti.grad.nutriscan.domain.foodlog.usecase

import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import javax.inject.Inject

class RemoveFoodEntryUseCase @Inject constructor(
    private val foodLogRepository: IFoodLogRepository
) {
    suspend operator fun invoke(entryId: String): Result<Unit> = foodLogRepository.removeFoodEntry(entryId)
}
