package iti.grad.nutriscan.domain.streak.repository

import kotlinx.coroutines.flow.Flow

interface IStreakRepository {
    fun observeStreak(): Flow<Int>
    suspend fun syncDailyStreak(): Result<Unit>
}
