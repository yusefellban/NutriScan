package iti.grad.nutriscan.domain.allergy.model

/**
 * Represents an allergy option returned by the backend, used during health
 * profile setup so the user can select the allergies that apply to them
 * (GET /v1/allergies).
 */
data class Allergy(
    val id: Int,
    val name: String,
    val description: String? = null
)
