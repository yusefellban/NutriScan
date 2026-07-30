package iti.grad.nutriscan.domain.steps.history.model

import java.time.LocalDate

data class StepHistorySummary(
    val periodAverage: Int,
    val stepGoal: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val monthlyData: List<MonthlyStepData>,
    val totalCaloriesBurned: Int,
    val totalDistanceKm: Double,
    val totalActiveMinutes: Int
)
