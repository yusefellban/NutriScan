package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.datasource.IAuthRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: IAuthRemoteDataSource,
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
