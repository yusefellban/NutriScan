package iti.grad.nutriscan.domain.nutrigpt.model

data class NutriGptSource(
    val fileName: String,
    val chunkIndex: Int,
    val score: Double,
    val snippet: String
)
