package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NutritionFactsDto(
    val calories: Long? = null,
    val proteinGrams: Float? = null,
    val carbsGrams: Float? = null,
    val fatG: Float? = null,
    val fiberGrams: Float? = null,
    val sugarG: Float? = null,
    val sodiumMg: Float? = null
)

@Serializable
data class ScanResultResponseDto(
    val scanId: String,
    val status: String,
    val scannedAt: String? = null,
    val imageUrl: String? = null,
    val foodSafetyResponse: FoodSafetyResponseDto? = null,
    val nutritionFacts: NutritionFactsDto? = null,
    val productName: String? = null,
    val favorite: Boolean? = false
)
