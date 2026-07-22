package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AllergyDto(
    val id: Int,
    val name: String,
    val description: String? = null
)
