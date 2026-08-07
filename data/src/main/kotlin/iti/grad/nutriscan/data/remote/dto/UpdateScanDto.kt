package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/** Request body for `PATCH /v1/scans/{scanId}`. */
@Serializable
data class UpdateScanDto(
    val name: String? = null,
    val favorite: Boolean,
)
