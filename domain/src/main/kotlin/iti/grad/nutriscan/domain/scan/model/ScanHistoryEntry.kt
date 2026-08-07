package iti.grad.nutriscan.domain.scan.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict

data class ScanHistoryEntry(
    val scanId: String,
    val imageUrl: String?,
    val verdict: ProductVerdict?,
    val scannedAt: String?,
    val productName: String?,
    val calories: Long?,
    val status: ScanStatus,
)
