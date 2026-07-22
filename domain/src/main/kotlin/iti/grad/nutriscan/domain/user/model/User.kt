package iti.grad.nutriscan.domain.user.model

data class User(
    val id: String,
    val firstName: String,
    val lastName: String?,
    val email: String,
    val gender: String?,
    val dateOfBirth: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val diseaseIds: List<Int>,
    val allergyIds: List<Int>,
    val avatarUrl: String? = null
)
