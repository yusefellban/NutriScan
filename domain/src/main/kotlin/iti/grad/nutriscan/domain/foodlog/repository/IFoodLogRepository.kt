package iti.grad.nutriscan.domain.foodlog.repository

import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import kotlinx.coroutines.flow.Flow

interface IFoodLogRepository {
    /** Empty flow if there's no authenticated user (see IAuthRepository.getCurrentUserId). */
    fun observeTodayFoodLog(): Flow<List<FoodLogEntry>>

    /** Writes locally and best-effort pushes to the backend (see DailyTrackingRepositoryImpl.pushMeal). */
    suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit>

    /** Writes locally only — no backend push. Used by ReconcileTodayUseCase to seed entries the
     * backend already knows about, avoiding a redundant/duplicate POST. */
    suspend fun addFoodEntryLocalOnly(entry: FoodLogEntry): Result<Unit>

    suspend fun removeFoodEntry(entryId: String): Result<Unit>
}
