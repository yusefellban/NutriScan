package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.steps.history.model.MonthlyStepData
import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.model.StepHistorySummary
import iti.grad.nutriscan.domain.steps.history.repository.IStepHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class StepHistoryRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IStepHistoryRepository {

    override suspend fun getStepHistory(period: StepHistoryPeriod): Result<StepHistorySummary> {
        return withContext(ioDispatcher) {
            val today = LocalDate.now()
            
            // Mock data based on period
            val summary = when (period) {
                StepHistoryPeriod.WEEK -> StepHistorySummary(
                    periodAverage = 5230,
                    stepGoal = 10_000,
                    startDate = today.minusDays(6),
                    endDate = today,
                    monthlyData = listOf(
                        MonthlyStepData("Mon", 4500),
                        MonthlyStepData("Tue", 5200),
                        MonthlyStepData("Wed", 3800),
                        MonthlyStepData("Thu", 6100),
                        MonthlyStepData("Fri", 4900),
                        MonthlyStepData("Sat", 7200),
                        MonthlyStepData("Sun", 4910)
                    ),
                    totalCaloriesBurned = 1250,
                    totalDistanceKm = 25.4,
                    totalActiveMinutes = 320
                )
                StepHistoryPeriod.MONTH -> StepHistorySummary(
                    periodAverage = 6100,
                    stepGoal = 10_000,
                    startDate = today.minusDays(29),
                    endDate = today,
                    monthlyData = listOf(
                        MonthlyStepData("Week 1", 42000),
                        MonthlyStepData("Week 2", 45000),
                        MonthlyStepData("Week 3", 41000),
                        MonthlyStepData("Week 4", 48000)
                    ),
                    totalCaloriesBurned = 5800,
                    totalDistanceKm = 120.5,
                    totalActiveMinutes = 1500
                )
                StepHistoryPeriod.THREE_MONTHS -> StepHistorySummary(
                    periodAverage = 5800,
                    stepGoal = 10_000,
                    startDate = today.minusMonths(3),
                    endDate = today,
                    monthlyData = listOf(
                        MonthlyStepData("May", 185000),
                        MonthlyStepData("Jun", 176862),
                        MonthlyStepData("Jul", 177633)
                    ),
                    totalCaloriesBurned = 25400,
                    totalDistanceKm = 450.2,
                    totalActiveMinutes = 5400
                )
                StepHistoryPeriod.SIX_MONTHS -> StepHistorySummary(
                    // From screenshots
                    periodAverage = 6361,
                    stepGoal = 10_000,
                    startDate = today.minusMonths(6),
                    endDate = today,
                    monthlyData = listOf(
                        MonthlyStepData("Jan", 34726),
                        MonthlyStepData("Feb", 168902),
                        MonthlyStepData("Mar", 210551),
                        MonthlyStepData("Apr", 192243),
                        MonthlyStepData("May", 196882),
                        MonthlyStepData("Jun", 176862),
                        MonthlyStepData("Jul", 177633)
                    ),
                    totalCaloriesBurned = 47277,
                    totalDistanceKm = 816.8,
                    totalActiveMinutes = 11578
                )
            }
            Result.success(summary)
        }
    }
}
