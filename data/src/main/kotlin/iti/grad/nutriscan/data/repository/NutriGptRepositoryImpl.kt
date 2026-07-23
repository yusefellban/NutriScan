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
import java.util.UUID
import javax.inject.Inject

class NutriGptRepositoryImpl @Inject constructor(
    private val apiService: NutriGptApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : INutriGptRepository {

    override suspend fun sendQuery(query: String) =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                apiService
                    .sendQuery(NutriGptRequestDto(query))
                    .toDomain()
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
