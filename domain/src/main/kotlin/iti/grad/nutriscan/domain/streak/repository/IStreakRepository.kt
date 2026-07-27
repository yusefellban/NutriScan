package iti.grad.nutriscan.domain.streak.repository

import iti.grad.nutriscan.domain.streak.model.StreakInfo
import kotlinx.coroutines.flow.Flow

interface IStreakRepository {
    fun observeStreak(): Flow<StreakInfo>

    /**
     * Recomputes streak from food-log activity. On failure the last known
     * streak value must be left untouched (fail closed, never reset to 0).
     */
    suspend fun recomputeStreak(): Result<Unit>
}
