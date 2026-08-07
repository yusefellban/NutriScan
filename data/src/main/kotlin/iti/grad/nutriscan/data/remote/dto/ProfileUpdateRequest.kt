package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequest(
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    val gender: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: String? = null,
    @SerialName("heightCm") val heightCm: Double? = null,
    @SerialName("weightKg") val weightKg: Double? = null,
    @SerialName("diseaseIds") val diseaseIds: List<Int>? = null,
    @SerialName("allergyIds") val allergyIds: List<Int>? = null
)
