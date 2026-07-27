package iti.grad.nutriscan.domain.dailytracking.model

import java.time.LocalDate

data class DailyTrackingRemoteSnapshot(
    val date: LocalDate,
    val targetWaterCnt: Int,
    val waterCnt: Int,
    val stepsCnt: Int,
    val meals: List<RemoteMealSnapshot>,
)
