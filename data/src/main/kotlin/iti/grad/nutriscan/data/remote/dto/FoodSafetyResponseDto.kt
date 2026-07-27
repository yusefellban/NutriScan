package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FlaggedIngredientDto(
    val ingredient: String? = null,
    val reason: String? = null,
    val type: String? = null,
    val name: List<String>? = emptyList()
)

@Serializable
data class FoodSafetyResponseDto(
    val verdict: String? = null,
    val flaggedIngredients: List<FlaggedIngredientDto> = emptyList(),
    val summary: String? = null,
)
