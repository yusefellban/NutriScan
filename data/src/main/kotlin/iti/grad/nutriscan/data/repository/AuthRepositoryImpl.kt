package iti.grad.nutriscan.data.repository

import android.net.Uri
import iti.grad.nutriscan.data.local.datasource.IAuthTokenLocalDataSource
import iti.grad.nutriscan.data.remote.api.KeycloakApiService
import iti.grad.nutriscan.data.remote.datasource.IAuthRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import kotlinx.serialization.json.Json
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: IAuthRemoteDataSource,
    private val keycloakApiService: KeycloakApiService,
    private val authTokenLocalDataSource: IAuthTokenLocalDataSource,
    private val json: Json
) : IAuthRepository {

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val request = RegisterRequestDto(
                firstName = "",
                lastName = "",
                email = email,
                username = email.substringBefore("@"),
                password = password,
                dateOfBirth = "",
                gender = "",
                heightCm = 0.0,
                weightKg = 0.0,
                allergies = emptyList(),
                diseases = emptyList()
            )

            val response = remoteDataSource.register(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            val request = ResendVerificationRequestDto(email = email)
            val response = remoteDataSource.resendVerification(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
            val config = getOidcConfig()
            
            val serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse(config.authorizationEndpoint),
                Uri.parse(config.tokenEndpoint)
            )

            val tokenRequest = TokenRequest.Builder(serviceConfig, config.clientId)
                .setGrantType("custom") // Not strictly used for token response parsing
                .build()

            val tokenResponse = TokenResponse.Builder(tokenRequest)
                .setAccessToken(authTokens.accessToken)
                .setRefreshToken(authTokens.refreshToken)
                .setIdToken(authTokens.idToken)
                .setAccessTokenExpirationTime(
                    authTokens.expiresIn?.let { System.currentTimeMillis() + (it * 1000) }
                )
                .build()

            val authState = AuthState(serviceConfig)
            authState.update(tokenResponse, null)
            
            authTokenLocalDataSource.saveAuthState(authState.jsonSerializeString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
}
