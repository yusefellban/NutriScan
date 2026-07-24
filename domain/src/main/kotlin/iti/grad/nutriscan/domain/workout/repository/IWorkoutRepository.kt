package iti.grad.nutriscan.domain.workout.repository

import kotlinx.coroutines.flow.Flow

interface IWorkoutRepository {
    /** Whether the user marked a workout done today. */
    fun observeTodayDone(): Flow<Boolean>
    suspend fun markDone(): Result<Unit>
}
