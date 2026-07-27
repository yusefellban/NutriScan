package iti.grad.nutriscan.domain.water.usecase

import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import javax.inject.Inject

class UnlogWaterGlassUseCase @Inject constructor(
    private val waterRepository: IWaterRepository
) {
    suspend operator fun invoke(): Result<Unit> = waterRepository.unlogGlass()
}
