package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String,
    val dateOfBirth: String,
    val gender: String,
    val heightCm: Double,
    val weightKg: Double,
    val allergies: List<AllergyItemDto>,
    val diseases: List<DiseaseItemDto>
)

@Serializable
data class AllergyItemDto(
    val id: Int,
    val name: String
)

@Serializable
data class DiseaseItemDto(
    val id: Int,
    val name: String
)
