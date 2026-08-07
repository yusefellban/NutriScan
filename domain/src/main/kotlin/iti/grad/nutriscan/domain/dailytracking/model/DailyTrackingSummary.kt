package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTrackingSummary(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val stepsKcal: Int,
    val exerciseKcal: Int,
    val exerciseMinutes: Int,
    val totalMealKcal: Int,
    val mealCount: Int,
)
