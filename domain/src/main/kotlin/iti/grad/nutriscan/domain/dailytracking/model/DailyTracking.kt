package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTracking(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val caloriesBurnedSteps: Int,
    val syncedToBackend: Boolean,
)
