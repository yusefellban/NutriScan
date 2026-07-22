package iti.grad.nutriscan.domain.user.repository

interface IUserRepository {
    /**
     * Persists the user's profile data selected during setup to their backend
     * profile (PATCH /v1/users/profile).
     */
    suspend fun updateHealthProfile(
        diseaseIds: List<Int>? = null,
        allergyIds: List<Int>? = null,
        gender: String? = null,
        dateOfBirth: String? = null,
        heightCm: Double? = null,
        weightKg: Double? = null
    ): Result<Unit>
}
