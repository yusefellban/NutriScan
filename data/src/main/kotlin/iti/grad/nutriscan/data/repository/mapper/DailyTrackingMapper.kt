package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingSummaryResponseDto
import iti.grad.nutriscan.domain.dailytracking.model.DailyTracking
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingRemoteSnapshot
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.model.RemoteMealSnapshot
import iti.grad.nutriscan.domain.common.CairoDateProvider
import java.time.LocalDate

fun DailyTrackingEntity.toDomain(): DailyTracking = DailyTracking(
    date = LocalDate.parse(date),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    exerciseKcal = exerciseKcal,
    exerciseMinutes = exerciseMinutes,
    syncedToBackend = syncedToBackend,
)

fun DailyTracking.toEntity(userId: String): DailyTrackingEntity = DailyTrackingEntity(
    userId = userId,
    date = date.toString(),
    targetWaterCnt = targetWaterCnt,
    waterCnt = waterCnt,
    stepsCnt = stepsCnt,
    caloriesBurnedSteps = caloriesBurnedSteps,
    exerciseKcal = exerciseKcal,
    exerciseMinutes = exerciseMinutes,
    syncedToBackend = syncedToBackend,
)

fun DailyTrackingResponseDto.toRemoteSnapshot(): DailyTrackingRemoteSnapshot = DailyTrackingRemoteSnapshot(
    date = date?.let { LocalDate.parse(it) } ?: CairoDateProvider.today(),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    meals = meals.mapNotNull { meal ->
        val scanId = meal.scanId ?: return@mapNotNull null
        RemoteMealSnapshot(
            scanId = scanId,
            productName = meal.productName,
            imageUrl = meal.imageUrl,
            calories = meal.nutritionFacts?.calories?.toInt() ?: 0,
            mealCnt = meal.mealCnt ?: 1,
        )
    },
)

fun DailyTrackingSummaryResponseDto.toDomain(): DailyTrackingSummary = DailyTrackingSummary(
    date = date?.let { LocalDate.parse(it) } ?: CairoDateProvider.today(),
    targetWaterCnt = targetWaterCnt ?: 0,
    waterCnt = waterCnt ?: 0,
    stepsCnt = stepsCnt ?: 0,
    stepsKcal = stepsKcal?.toInt() ?: 0,
    exerciseKcal = exerciseKcal?.toInt() ?: 0,
    exerciseMinutes = exerciseMin?.toInt() ?: 0,
    totalMealKcal = totalMealKcal?.toInt() ?: 0,
    mealCount = mealCount ?: 0,
)
