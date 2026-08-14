package iti.grad.nutriscan.data.repository

import android.net.Uri
import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.local.util.JwtDecoder
import iti.grad.nutriscan.data.remote.api.KeycloakApiService
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import iti.grad.nutriscan.data.remote.datasource.IAuthRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ForgotPasswordRequestDto
import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.DomainException
import kotlinx.serialization.json.Json

import timber.log.Timber
import javax.inject.Inject

import iti.grad.nutriscan.data.db.NutriScanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: IAuthRemoteDataSource,
    private val keycloakApiService: KeycloakApiService,
    private val tokenRefreshApiService: TokenRefreshApiService,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val database: NutriScanDatabase
) : IAuthRepository {

    override suspend fun register(firstName: String, lastName: String, email: String, password: String): Result<Unit> {
        return try {
            val username = email.substringBefore("@")
            val request = RegisterRequestDto(
                firstName = firstName,
                lastName = lastName,
                email = email,
                username = username,
                password = password,
                dateOfBirth = "2000-01-01",
                gender = "MALE",
                heightCm = 170.0,
                weightKg = 170.0,
                allergies = emptyList(),
                diseases = emptyList()
            )

            Timber.d("Registration attempt for email: $email, username: $username")
            val response = remoteDataSource.register(request)

            if (response.isSuccessful) {
                Timber.d("Registration successful for email: $email")
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Registration failed with code: ${response.code()}, errorBody: $rawError")
                val errorMessage = parseErrorMessage(rawError)
                Result.failure(mapToDomainException(response.code(), rawError, errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during registration")
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            val request = ResendVerificationRequestDto(email = email)
            val response = remoteDataSource.resendVerification(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(rawError)
                Result.failure(mapToDomainException(response.code(), rawError, errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val response = keycloakApiService.loginWithEmail(username = email, password = password)
            if (response.isSuccessful) {
                val dto = response.body() ?: throw Exception("Empty response body from Keycloak")
                val authTokens = AuthTokens(
                    accessToken = dto.accessToken,
                    refreshToken = dto.refreshToken,
                    idToken = dto.idToken,
                    expiresIn = dto.expiresIn,
                    refreshExpiresIn = dto.refreshExpiresIn
                )
                saveTokens(authTokens)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(rawError)
                Result.failure(mapToDomainException(response.code(), rawError, errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun getOidcConfig(): OidcAuthConfig {
        return OidcAuthConfig(
            authorizationEndpoint = "https://auth.nutriscan.dev/realms/nutriscan/protocol/openid-connect/auth",
            tokenEndpoint = "https://auth.nutriscan.dev/realms/nutriscan/protocol/openid-connect/token",
            clientId = "mobile-api",
            redirectUri = "nutriscan://oauth2callback"
        )
    }

    override suspend fun saveTokens(authTokens: AuthTokens): Result<Unit> {
        return try {
            val access = authTokens.accessToken ?: throw Exception("Missing access token")
            val refresh = authTokens.refreshToken ?: throw Exception("Missing refresh token")
            tokenManager.saveTokens(access, refresh, authTokens.idToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapExceptionToDomain(e))
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val request = ForgotPasswordRequestDto(email = email)
            val response = remoteDataSource.forgotPassword(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(rawError)
                Result.failure(mapToDomainException(response.code(), rawError, errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(mapExceptionToDomain(e))
        }
    }

    /**
     * Attempts to parse the backend Standard Error Format JSON from the error body.
     * Falls back to a generic message if parsing fails.
     */
    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "An unexpected error occurred."
        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
            // If there are field-level validation details, include the first one
            val detailSuffix = apiError.details?.firstOrNull()?.let { detail ->
                " (${detail.field}: ${detail.issue})"
            }.orEmpty()
            apiError.message + detailSuffix
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    private fun mapToDomainException(code: Int, rawError: String?, parsedMessage: String): DomainException {
        return when {
            code == 401 || rawError?.contains("invalid_grant") == true -> DomainException.UnauthorizedException(parsedMessage)
            code >= 500 -> DomainException.ServerException(parsedMessage)
            else -> DomainException.UnknownException(parsedMessage)
        }
    }

    private fun mapExceptionToDomain(e: Exception): DomainException {
        return if (e is java.io.IOException) {
            DomainException.NetworkException(cause = e)
        } else {
            DomainException.UnknownException(cause = e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return !tokenManager.getAccessToken().isNullOrBlank()
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                tokenRefreshApiService.logout(refreshToken = refreshToken)
            }
            tokenManager.clearTokens()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            Result.success(Unit)
        }
    }

    /**
     * The `sub` claim identifies the signed-in account and scopes every per-user Room table.
     *
     * Falls back to the access token because the login request sends no `scope=openid`, so
     * Keycloak never issues an id_token and [TokenManager.getIdToken] is always null. That made
     * this return null for every signed-in user, and each caller then fell back to a shared
     * device-local id — so every account on the device read and wrote the *same* rows. Both tokens
     * are JWTs carrying the same `sub`, so reading it from the access token needs no realm or
     * client change.
     */
    override suspend fun getCurrentUserId(): String? =
        JwtDecoder.extractSubjectClaim(tokenManager.getIdToken())
            ?: JwtDecoder.extractSubjectClaim(tokenManager.getAccessToken())
    override suspend fun getAccessToken(): String? = tokenManager.getAccessToken()
}
