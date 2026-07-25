package iti.grad.nutriscan.domain.nutrigpt.model

data class NutriGptMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val sources: List<NutriGptSource> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
