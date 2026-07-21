package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: IUserRemoteDataSource,
    private val json: Json
) : IUserRepository {

    override suspend fun updateHealthProfile(diseaseIds: List<Int>, allergyIds: List<Int>): Result<Unit> {
        return try {
            val request = UpdateUserProfileRequestDto(diseaseIds = diseaseIds, allergyIds = allergyIds)
            val response = remoteDataSource.updateProfile(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Update diseases failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while updating diseases")
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "An unexpected error occurred."
        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
            apiError.message
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }
}
