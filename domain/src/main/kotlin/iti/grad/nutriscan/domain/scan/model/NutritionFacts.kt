package iti.grad.nutriscan.domain.scan.model

data class NutritionFacts(
    val calories: Long,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatG: Float,
    val fiberGrams: Float,
    val sugarG: Float,
    val sodiumMg: Float,
)
