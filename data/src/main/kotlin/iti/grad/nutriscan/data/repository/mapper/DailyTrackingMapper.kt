package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingSummaryResponseDto
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.model.RemoteMealSnapshot
import java.time.LocalDate

fun DailyTrackingEntity.toDomain(): DailyTracking = DailyTracking(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    syncedToBackend = syncedToBackend,
)

fun DailyTracking.toEntity(userId: String): DailyTrackingEntity = DailyTrackingEntity(
    userId = userId,
    date = date.toString(),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    syncedToBackend = syncedToBackend,
)

fun DailyTrackingResponseDto.toRemoteSnapshot(): DailyTrackingRemoteSnapshot = DailyTrackingRemoteSnapshot(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    meals = meals.map {
        RemoteMealSnapshot(
            scanId = it.scanId,
            productName = it.productName,
            imageUrl = it.imageUrl,
            calories = it.nutritionFacts?.calories?.toInt() ?: 0,
        )
    },
)

fun DailyTrackingSummaryResponseDto.toDomain(): DailyTrackingSummary = DailyTrackingSummary(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    mealCount = mealCount,
)
