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
    val avatarUrl: String? = null,
    /** Server-computed Body Mass Index. Read-only — never sent to the backend. */
    val bmi: Double? = null,
    /** Server-computed Total Daily Energy Expenditure in kcal/day. Read-only — never sent to the backend. */
    val tdee: Double? = null,
)
