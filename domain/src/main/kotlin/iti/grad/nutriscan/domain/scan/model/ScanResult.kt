package iti.grad.nutriscan.domain.scan.model

/**
 * A completed or in-progress scan from the backend.
 *
 * ⚠️ Health-critical: [foodSafetyResponse] must NEVER be silently defaulted
 * to a safe verdict. A null [foodSafetyResponse] means the scan is still
 * processing or has failed — do not infer safety from absence of data.
 */
data class ScanResult(
    val scanId: String,
    val status: ScanStatus,
    val scannedAt: String?,
    val imageUrl: String?,
    val foodSafetyResponse: FoodSafetyResponse?,
    val nutritionFacts: NutritionFacts?,
)
