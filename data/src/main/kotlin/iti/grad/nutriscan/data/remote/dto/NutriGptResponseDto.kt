package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NutriGptResponseDto(
    val query: String,
    val answer: String,
    val sources: List<NutriGptSourceDto> = emptyList()
)

@Serializable
data class NutriGptSourceDto(
    val fileName: String,
    val chunkIndex: Int,
    val score: Double,
    val snippet: String
)
