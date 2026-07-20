package iti.grad.nutriscan.domain.auth.repository

interface IAuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
}
