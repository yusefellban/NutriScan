package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request body for POST /v1/scans/barcode.
 *
 * @param barcode The raw barcode string detected by ML Kit (e.g. "5922157657516").
 */
@Serializable
data class BarcodeScanRequestDto(
    val barcode: String,
)
