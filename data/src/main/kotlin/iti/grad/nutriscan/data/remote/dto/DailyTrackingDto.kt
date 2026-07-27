package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyTrackingRequestDto(
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
)

@Serializable
data class DailyTrackingResponseDto(
    val id: Int? = null,
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
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
    val scanId: String,
    val productName: String? = null,
    val imageUrl: String? = null,
    val mealCnt: Int = 1,
    val nutritionFacts: NutritionFactsDto? = null,
)

@Serializable
data class DailyTrackingSummaryResponseDto(
    val id: Int? = null,
    val date: String,
    val targetWaterCnt: Int? = null,
    val waterCnt: Int? = null,
    val stepsCnt: Int? = null,
    val mealCount: Int = 0,
)

@Serializable
data class PageDailyTrackingSummaryResponseDto(
    val content: List<DailyTrackingSummaryResponseDto> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
)
