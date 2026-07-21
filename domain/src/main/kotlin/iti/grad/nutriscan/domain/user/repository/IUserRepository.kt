package iti.grad.nutriscan.domain.user.repository

interface IUserRepository {
    /**
     * Persists the diseases and allergies the user selected during health
     * profile setup to their backend profile (PATCH /v1/users/profile).
     */
    suspend fun updateHealthProfile(diseaseIds: List<Int>, allergyIds: List<Int>): Result<Unit>
}
