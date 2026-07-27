package iti.grad.nutriscan.domain.dailytracking.usecase

import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import javax.inject.Inject

class AddExerciseWorkoutUseCase @Inject constructor(
    private val repository: IDailyTrackingRepository,
) {
    suspend operator fun invoke(kcalBurned: Int, minutes: Int): Result<Unit> =
        repository.addExerciseWorkout(kcalBurned, minutes)
}
