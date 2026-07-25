package iti.grad.nutriscan.domain.exercises.model

/**
 * One page of exercises returned by `GET /exercises`, with pagination metadata so the
 * presentation layer can drive "load more" / infinite-scroll without re-deriving it from
 * raw DTOs.
 */
data class ExercisePage(
    val exercises: List<Exercise>,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean,
)
