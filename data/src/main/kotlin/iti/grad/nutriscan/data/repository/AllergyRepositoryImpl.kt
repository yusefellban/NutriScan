package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.datasource.IAllergyRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class AllergyRepositoryImpl @Inject constructor(
    private val remoteDataSource: IAllergyRemoteDataSource,
    private val json: Json
) : IAllergyRepository {

    override suspend fun getAllergies(): Result<List<Allergy>> {
        return try {
            val response = remoteDataSource.getAllergies()
            if (response.isSuccessful) {
                val allergies = response.body().orEmpty().map { dto ->
                    Allergy(id = dto.id, name = dto.name, description = dto.description)
                }
                Result.success(allergies)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Get allergies failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching allergies")
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
