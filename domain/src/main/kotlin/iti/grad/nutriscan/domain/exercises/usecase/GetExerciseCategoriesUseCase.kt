package iti.grad.nutriscan.domain.exercises.usecase

import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import javax.inject.Inject

class GetExerciseCategoriesUseCase @Inject constructor(
    private val repository: IExercisesRepository,
) {
    suspend operator fun invoke(): Result<List<String>> = repository.getCategories()
}
