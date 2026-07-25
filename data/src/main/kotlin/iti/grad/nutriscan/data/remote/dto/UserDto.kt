package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    val email: String,
    val gender: String? = null,
    @SerialName("dateOfBirth") val dateOfBirth: String? = null,
    @SerialName("heightCm") val heightCm: Double? = null,
    @SerialName("weightKg") val weightKg: Double? = null,
    @SerialName("diseaseIds") val diseaseIds: List<Int>? = null,
    @SerialName("allergyIds") val allergyIds: List<Int>? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("familyMembers") val familyMembers: List<FamilyMemberDto>? = null,
    val allergies: List<AllergyDto> = emptyList(),
    val diseases: List<DiseaseDto> = emptyList()
)
