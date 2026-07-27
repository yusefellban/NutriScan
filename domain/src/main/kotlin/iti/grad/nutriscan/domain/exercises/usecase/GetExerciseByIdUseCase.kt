package iti.grad.nutriscan.domain.exercises.usecase

import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import javax.inject.Inject

class GetExerciseByIdUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(id: String): Result<Exercise> = repository.getExerciseById(id)
}
