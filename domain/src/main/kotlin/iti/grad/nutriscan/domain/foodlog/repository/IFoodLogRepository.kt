package iti.grad.nutriscan.domain.foodlog.repository

import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import kotlinx.coroutines.flow.Flow

interface IFoodLogRepository {
    /** Empty flow if there's no authenticated user (see IAuthRepository.getCurrentUserId). */
    fun observeTodayFoodLog(): Flow<List<FoodLogEntry>>

    suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit>

    suspend fun removeFoodEntry(entryId: String): Result<Unit>
}
