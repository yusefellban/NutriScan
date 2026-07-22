package iti.grad.nutriscan.domain.user.model

data class ProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val diseaseIds: List<Int>? = null,
    val allergyIds: List<Int>? = null,
    val avatarUrl: String? = null
)
