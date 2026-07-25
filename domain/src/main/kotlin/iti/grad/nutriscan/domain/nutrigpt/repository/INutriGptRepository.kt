package iti.grad.nutriscan.domain.nutrigpt.repository

import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import kotlinx.coroutines.flow.StateFlow

interface INutriGptRepository {
    val messages: StateFlow<List<NutriGptMessage>>
    
    fun addMessage(message: NutriGptMessage)
    
    fun clearMessages()
    
    suspend fun sendQuery(query: String): Result<NutriGptMessage>
}
