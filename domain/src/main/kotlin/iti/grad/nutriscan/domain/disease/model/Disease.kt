package iti.grad.nutriscan.domain.disease.model

/**
 * Represents a chronic disease/condition option returned by the backend,
 * used during health profile setup so the user can select the conditions
 * that apply to them (GET /v1/diseases).
 */
data class Disease(
    val id: Int,
    val name: String,
    val description: String? = null
)
