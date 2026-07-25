package iti.grad.nutriscan.domain.exercises.repository

import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.model.ExercisePage
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery

/**
 * Only the three endpoints actually used by ExercisesScreen / ExerciseWorkoutScreen today
 * are exposed here (`/exercises`, `/exercises/{id}`, `/exercises/categories`).
 * `/batch`, `/random`, `/suggestions`, `/stats`, `/equipments`, `/targets` and static
 * image/gif proxying are intentionally not implemented — this interface can grow to cover
 * them later without breaking anything already built against it.
 */
interface IExercisesRepository {
    suspend fun getExercises(query: ExerciseQuery): Result<ExercisePage>
    suspend fun getExerciseById(id: String): Result<Exercise>
    suspend fun getCategories(): Result<List<String>>
}
