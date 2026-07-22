package iti.grad.nutriscan.domain.steps.repository

import kotlinx.coroutines.flow.Flow

interface IStepsRepository {
    /** Live count of steps taken since local midnight, read from the device's step-counter sensor. */
    fun observeTodaySteps(): Flow<Int>

    suspend fun hasStepsPermission(): Boolean
}
