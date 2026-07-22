package iti.grad.nutriscan.domain.steps.usecase

import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import javax.inject.Inject

class CheckStepsPermissionUseCase @Inject constructor(
    private val stepsRepository: IStepsRepository
) {
    suspend operator fun invoke(): Boolean = stepsRepository.hasStepsPermission()
}
