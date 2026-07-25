package iti.grad.nutriscan.domain.nutrigpt.usecase

import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import iti.grad.nutriscan.domain.nutrigpt.repository.INutriGptRepository
import javax.inject.Inject

class SendNutriGptMessageUseCase @Inject constructor(
    private val repository: INutriGptRepository
) {
    suspend operator fun invoke(query: String): Result<NutriGptMessage> {
        return repository.sendQuery(query)
    }
}
