package iti.grad.nutriscan.domain.exercises.usecase

import iti.grad.nutriscan.domain.exercises.model.ExercisePage
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import javax.inject.Inject

class GetExercisesUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(query: ExerciseQuery): Result<ExercisePage> =
        repository.getExercises(query)
}
