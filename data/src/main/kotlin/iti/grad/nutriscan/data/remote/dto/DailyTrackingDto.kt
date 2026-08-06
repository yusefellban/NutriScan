package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyTrackingRequestDto(
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val stepsKcal: Double? = null,
    val exerciseKcal: Double? = null,
    val exerciseMin: Double? = null,
)

@Serializable
data class DailyTrackingResponseDto(
    val id: Int? = null,
    val date: String? = null,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val stepsKcal: Double? = null,
    val exerciseKcal: Double? = null,
    val exerciseMin: Double? = null,
    val totalMealKcal: Long? = null,
    val meals: List<DailyTrackingMealResponseDto> = emptyList(),
)

@Serializable
data class DailyTrackingMealRequestDto(
    val scanId: String,
    val mealCnt: Int,
)

@Serializable
data class UpdateMealRequestDto(
    val mealCnt: Int,
)

@Serializable
data class DailyTrackingMealResponseDto(
    val scanId: String? = null,
    val productName: String? = null,
    val imageUrl: String? = null,
    val mealCnt: Int? = null,
    val nutritionFacts: NutritionFactsDto? = null,
)

@Serializable
data class DailyTrackingSummaryResponseDto(
    val id: Int? = null,
    val date: String? = null,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val stepsKcal: Double? = null,
    val exerciseKcal: Double? = null,
    val exerciseMin: Double? = null,
    val totalMealKcal: Long? = null,
    val mealCount: Int? = null,
)

@Serializable
data class PageDailyTrackingSummaryResponseDto(
    val content: List<DailyTrackingSummaryResponseDto> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val last: Boolean = true,
)
