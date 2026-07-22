package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleSourceDto(
    val id: String? = null,
    val name: String? = null,
)
