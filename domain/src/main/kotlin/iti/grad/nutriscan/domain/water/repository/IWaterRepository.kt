package iti.grad.nutriscan.domain.water.repository

import iti.grad.nutriscan.domain.water.model.WaterLog
import kotlinx.coroutines.flow.Flow

interface IWaterRepository {
    /** Live water progress for today, resets at local midnight. */
    fun observeToday(): Flow<WaterLog>
    suspend fun logGlass(): Result<Unit>
    suspend fun unlogGlass(): Result<Unit>
    suspend fun setGoal(glasses: Int): Result<Unit>
}
