package iti.grad.nutriscan.data.remote.dto

import iti.grad.nutriscan.domain.common.model.ProductVerdict
import kotlinx.serialization.Serializable

/**
 * Lightweight DTO for a single item in the paginated scan history list
 * returned by GET /v1/scans.
 *
 * This is intentionally distinct from [ScanResultResponseDto], which represents
 * the full detail response from GET /v1/scans/{scanId} and has a different shape
 * (verdict is nested inside foodSafetyResponse there).
 */
@Serializable
data class ScanHistoryItemDto(
    val scanId: String,
    val imageUrl: String? = null,
    val verdict: ProductVerdict? = null,
    val scannedAt: String? = null,
    val productName: String? = null,
    val calories: Long? = null,
    val status: String,
)
