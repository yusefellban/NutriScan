package iti.grad.nutriscan.domain.water.usecase

import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import javax.inject.Inject

class SetWaterGoalUseCase @Inject constructor(
    private val waterRepository: IWaterRepository
) {
    suspend operator fun invoke(glasses: Int): Result<Unit> = waterRepository.setGoal(glasses)
}
