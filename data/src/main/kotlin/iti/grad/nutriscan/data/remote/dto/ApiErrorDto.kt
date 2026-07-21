package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Matches the backend Standard Error Format used by every endpoint on failure.
 * Used to parse HTTP error response bodies (400, 409, etc.) into structured data.
 */
@Serializable
data class ApiErrorDto(
    val timestamp: String = "",
    val status: Int = 0,
    val error: String = "",
    val message: String = "",
    val details: List<ApiErrorDetailDto>? = null,
    val path: String = ""
)

@Serializable
data class ApiErrorDetailDto(
    val field: String = "",
    val issue: String = ""
)
