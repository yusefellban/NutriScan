package iti.grad.nutriscan.domain.exercises.model

/**
 * Centralized filter object for `GetExercisesUseCase` instead of scattering raw
 * page/limit/bodyPart/query params through every layer. The search box and category
 * chip row both build one of these and pass it straight through to the repository.
 */
data class ExerciseQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val bodyPart: String? = null,
    val query: String? = null,
)
