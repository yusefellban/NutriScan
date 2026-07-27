package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTrackingSummary(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val mealCount: Int,
)
