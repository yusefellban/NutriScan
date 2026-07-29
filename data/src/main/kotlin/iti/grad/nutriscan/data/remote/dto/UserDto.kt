package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    val username: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bmi: Double? = null,
    val tdee: Double? = null,
    val allergies: List<DiseaseOrAllergyDto>? = null,
    val diseases: List<DiseaseOrAllergyDto>? = null,
    val familyMembers: List<FamilyMemberDto>? = null,
    /** Backend serializes the avatar picture field as "imageUrl". */
    @SerialName("imageUrl") val avatarUrl: String? = null,
    /** Used purely for cache-busting the avatar image locally — see [iti.grad.nutriscan.data.repository.UserRepositoryImpl]. */
    val updatedAt: String? = null
)

@Serializable
data class DiseaseOrAllergyDto(
    val id: Int,
    val name: String
)
