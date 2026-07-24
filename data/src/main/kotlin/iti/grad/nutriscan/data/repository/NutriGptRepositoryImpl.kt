package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.NutriGptApiService
import iti.grad.nutriscan.data.remote.dto.NutriGptRequestDto
import iti.grad.nutriscan.data.remote.dto.NutriGptResponseDto
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptSource
import iti.grad.nutriscan.domain.nutrigpt.repository.INutriGptRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class NutriGptRepositoryImpl @Inject constructor(
    private val apiService: NutriGptApiService,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : INutriGptRepository {

    override suspend fun sendQuery(query: String): Result<NutriGptMessage> =
        withContext(ioDispatcher) {
            try {
                Timber.d("Sending NutriGPT query")
                val response = apiService.sendQuery(NutriGptRequestDto(query))
                
                if (response.isSuccessful) {
                    Timber.d("NutriGPT query successful")
                    val message = response.body()?.toDomain() ?: throw Exception("Empty response body")
                    Result.success(message)
                } else {
                    val rawError = response.errorBody()?.string()
                    Timber.e("NutriGPT query failed with code: ${response.code()}, errorBody: $rawError")
                    val errorMessage = parseErrorMessage(rawError)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during NutriGPT query")
                Result.failure(e)
            }
        }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "An unexpected error occurred."
        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
            val detailSuffix = apiError.details?.firstOrNull()?.let { detail ->
                " (${detail.field}: ${detail.issue})"
            }.orEmpty()
            apiError.message + detailSuffix
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    private fun NutriGptResponseDto.toDomain(): NutriGptMessage = NutriGptMessage(
        id = UUID.randomUUID().toString(),
        text = answer,
        isFromUser = false,
        sources = sources.map { sourceDto ->
            NutriGptSource(
                fileName = sourceDto.fileName,
                chunkIndex = sourceDto.chunkIndex,
                score = sourceDto.score,
                snippet = sourceDto.snippet
            )
        }
    )
}
