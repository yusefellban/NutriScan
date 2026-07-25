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
    val familyMembers: List<String>? = null, // Adjust type if known
    val avatarUrl: String? = null
)

@Serializable
data class DiseaseOrAllergyDto(
    val id: Int,
    val name: String
)
