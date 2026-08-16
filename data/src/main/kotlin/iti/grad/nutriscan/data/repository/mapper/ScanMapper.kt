package iti.grad.nutriscan.data.repository.mapper

import iti.grad.nutriscan.data.remote.dto.FlaggedIngredientDto
import iti.grad.nutriscan.data.remote.dto.FamilyAlertDto
import iti.grad.nutriscan.data.remote.dto.FoodSafetyResponseDto
import iti.grad.nutriscan.data.remote.dto.NutritionFactsDto
import iti.grad.nutriscan.data.remote.dto.ScanResultResponseDto
import iti.grad.nutriscan.data.remote.dto.ScanSubmissionResponseDto
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.domain.scan.model.FamilyAlert
import iti.grad.nutriscan.domain.scan.model.FoodSafetyResponse
import iti.grad.nutriscan.domain.scan.model.NutritionFacts
import iti.grad.nutriscan.domain.scan.model.ScanFlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.model.ScanStatus

fun ScanSubmissionResponseDto.toDomain(): ScanResult {
    return ScanResult(
        scanId = scanId,
        status = mapStatus(status),
        scannedAt = null,
        imageUrl = null,
        foodSafetyResponse = null,
        nutritionFacts = null,
        productName = null,
        favorite = false
    )
}

fun ScanResultResponseDto.toDomain(): ScanResult {
    return ScanResult(
        scanId = scanId,
        status = mapStatus(status),
        scannedAt = scannedAt,
        imageUrl = imageUrl,
        foodSafetyResponse = foodSafetyResponse?.toDomain(),
        nutritionFacts = nutritionFacts?.toDomain(),
        productName = productName,
        favorite = favorite ?: false
    )
}

fun ScanHistoryItemDto.toDomain(): ScanHistoryEntry {
    return ScanHistoryEntry(
        scanId = scanId,
        imageUrl = imageUrl,
        verdict = verdict,
        scannedAt = scannedAt,
        productName = productName,
        calories = calories,
        status = mapStatus(status),
    )
}

internal fun mapStatus(status: String): ScanStatus {
    return when (status.uppercase()) {
        "COMPLETED" -> ScanStatus.COMPLETED
        "FAILED" -> ScanStatus.FAILED
        else -> ScanStatus.PROCESSING
    }
}

private fun FoodSafetyResponseDto.toDomain(): FoodSafetyResponse {
    return FoodSafetyResponse(
        verdict = verdict?.let { mapVerdict(it) },
        flaggedIngredients = flaggedIngredients.map { it.toDomain() },
        summary = summary,
        familyAlerts = familyAlerts.map { it.toDomain() },
    )
}

private fun FamilyAlertDto.toDomain(): FamilyAlert {
    return FamilyAlert(
        targetProfile = targetProfile ?: "",
        // Use mapVerdict for consistency — defaults to SAFE for unknown strings per backend contract.
        severity = severity?.let { mapVerdict(it) } ?: ProductVerdict.UNSAFE,
        reason = reason ?: "",
    )
}

internal fun mapVerdict(verdict: String): ProductVerdict {
    return when (verdict.uppercase()) {
        "SAFE" -> ProductVerdict.SAFE
        "CAUTION" -> ProductVerdict.CAUTION
        "UNSAFE" -> ProductVerdict.UNSAFE
        else -> ProductVerdict.SAFE // Default if unknown, though shouldn't happen based on backend specs
    }
}

private fun FlaggedIngredientDto.toDomain(): ScanFlaggedIngredient {
    return ScanFlaggedIngredient(
        ingredient = ingredient ?: "",
        reason = reason ?: "",
        type = type ?: "",
        name = name ?: emptyList()
    )
}

private fun NutritionFactsDto.toDomain(): NutritionFacts {
    return NutritionFacts(
        calories = calories ?: 0,
        proteinGrams = proteinGrams ?: 0f,
        carbsGrams = carbsGrams ?: 0f,
        fatG = fatG ?: 0f,
        fiberGrams = fiberGrams ?: 0f,
        sugarG = sugarG ?: 0f,
        sodiumMg = sodiumMg ?: 0f
    )
}
