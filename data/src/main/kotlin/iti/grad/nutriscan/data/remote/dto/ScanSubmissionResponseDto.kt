package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScanSubmissionResponseDto(
    val scanId: String,
    val status: String,
)
