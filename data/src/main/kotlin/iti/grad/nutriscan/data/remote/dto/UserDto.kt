package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val gender: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("disease_ids") val diseaseIds: List<Int>? = null,
    @SerialName("allergy_ids") val allergyIds: List<Int>? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("family_members") val familyMembers: List<FamilyMemberDto>? = null
)
