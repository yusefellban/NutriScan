package iti.grad.nutriscan.domain.exercises.model

/**
 * Pure domain model for a single exercise — no Android/UI types.
 *
 * `instructions` / `instructionSteps` are always the `en` locale content from the API
 * (the API has no `ar` translations — see the Exercises backend integration plan §2.1).
 * `repKcal` / `minKcal` are never null at this layer: [iti.grad.nutriscan.data.repository
 * .ExercisesRepositoryImpl] fills in conservative flat estimates whenever the API value
 * is null (see plan §2.3).
 */
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: String,
    val instructionSteps: List<String>,
    val imageUrl: String?,
    val gifUrl: String?,
    val repKcal: Double?,
    val minKcal: Double?,
)
