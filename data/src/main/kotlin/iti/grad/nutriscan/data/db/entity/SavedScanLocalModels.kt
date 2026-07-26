package iti.grad.nutriscan.data.db.entity

import kotlinx.serialization.Serializable

@Serializable
data class FlaggedIngredientLocalModel(
    val ingredient: String,
    val reason: String,
    val type: String,
    val name: List<String>
)

@Serializable
data class NutritionFactsLocalModel(
    val calories: Long,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatG: Float,
    val fiberGrams: Float,
    val sugarG: Float,
    val sodiumMg: Float
)
