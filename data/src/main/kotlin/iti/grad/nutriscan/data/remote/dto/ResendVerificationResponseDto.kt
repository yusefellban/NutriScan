package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResendVerificationResponseDto(
    val message: String
)
