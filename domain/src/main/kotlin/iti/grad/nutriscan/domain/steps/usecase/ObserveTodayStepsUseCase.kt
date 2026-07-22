package iti.grad.nutriscan.domain.steps.usecase

import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayStepsUseCase @Inject constructor(
    private val stepsRepository: IStepsRepository
) {
    operator fun invoke(): Flow<Int> = stepsRepository.observeTodaySteps()
}
