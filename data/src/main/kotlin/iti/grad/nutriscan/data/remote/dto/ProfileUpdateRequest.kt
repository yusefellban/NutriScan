package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequest(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val gender: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("disease_ids") val diseaseIds: List<Int>? = null,
    @SerialName("allergy_ids") val allergyIds: List<Int>? = null
)
