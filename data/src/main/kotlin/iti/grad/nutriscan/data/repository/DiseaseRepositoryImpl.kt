package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.datasource.IDiseaseRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class DiseaseRepositoryImpl @Inject constructor(
    private val remoteDataSource: IDiseaseRemoteDataSource,
    private val json: Json
) : IDiseaseRepository {

    override suspend fun getDiseases(): Result<List<Disease>> {
        return try {
            val response = remoteDataSource.getDiseases()
            if (response.isSuccessful) {
                val diseases = response.body().orEmpty().map { dto ->
                    Disease(id = dto.id, name = dto.name, description = dto.description)
                }
                Result.success(diseases)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Get diseases failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching diseases")
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
