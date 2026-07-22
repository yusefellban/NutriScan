package iti.grad.nutriscan.domain.auth.repository

import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig

interface IAuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun loginWithEmail(email: String, password: String): Result<Unit>
    suspend fun getOidcConfig(): OidcAuthConfig
    suspend fun saveTokens(authTokens: AuthTokens): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun logout(): Result<Unit>

    /** Decodes the `sub` claim from the stored ID token. Null if not logged in or undecodable. */
    suspend fun getCurrentUserId(): String?
}
