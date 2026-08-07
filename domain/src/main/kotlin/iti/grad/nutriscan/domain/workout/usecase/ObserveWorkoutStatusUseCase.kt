package iti.grad.nutriscan.domain.workout.usecase

import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorkoutStatusUseCase @Inject constructor(
    private val workoutRepository: IWorkoutRepository
) {
    operator fun invoke(): Flow<Boolean> = workoutRepository.observeTodayDone()
}
