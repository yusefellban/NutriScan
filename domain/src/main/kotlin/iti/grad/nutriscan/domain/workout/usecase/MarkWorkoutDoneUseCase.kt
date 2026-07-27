package iti.grad.nutriscan.domain.workout.usecase

import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import javax.inject.Inject

class MarkWorkoutDoneUseCase @Inject constructor(
    private val workoutRepository: IWorkoutRepository
) {
    suspend operator fun invoke(): Result<Unit> = workoutRepository.markDone()
}
