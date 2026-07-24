package iti.grad.nutriscan.domain.water.usecase

import iti.grad.nutriscan.domain.water.model.WaterLog
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTodayWaterUseCase @Inject constructor(
    private val waterRepository: IWaterRepository
) {
    operator fun invoke(): Flow<WaterLog> = waterRepository.observeToday()
}
