package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FlaggedIngredientDto(
    val ingredient: String? = null,
    val reason: String? = null,
    val type: String? = null,
    val name: List<String>? = emptyList()
)

/**
 * Per-family-member safety alert returned by the AI analysis pipeline.
 * Health-critical: must never be silently dropped — maps 1-to-1 to [FamilyAlert] domain model.
 */
@Serializable
data class FamilyAlertDto(
    val targetProfile: String? = null,
    val severity: String? = null,
    val reason: String? = null,
)

@Serializable
data class FoodSafetyResponseDto(
    val verdict: String? = null,
    val flaggedIngredients: List<FlaggedIngredientDto> = emptyList(),
    val summary: String? = null,
    val familyAlerts: List<FamilyAlertDto> = emptyList(),
)
