package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Generic wrapper for Spring Boot's paginated Page<T> response.
 *
 * The `pageable` and `sort` nested objects from the backend are intentionally
 * omitted — they are ignored by the kotlinx.serialization Json config
 * (ignoreUnknownKeys = true).
 */
@Serializable
data class PageDto<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean,
)
