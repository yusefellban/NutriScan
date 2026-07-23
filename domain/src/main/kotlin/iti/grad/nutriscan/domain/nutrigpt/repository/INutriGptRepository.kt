package iti.grad.nutriscan.domain.nutrigpt.repository

import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage

interface INutriGptRepository {
    suspend fun sendQuery(query: String): Result<NutriGptMessage>
}
