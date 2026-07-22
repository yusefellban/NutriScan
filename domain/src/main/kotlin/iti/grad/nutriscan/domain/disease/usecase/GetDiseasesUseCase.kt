package iti.grad.nutriscan.domain.disease.usecase

import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiseasesUseCase @Inject constructor(
    private val diseaseRepository: IDiseaseRepository
) {
    operator fun invoke(): Flow<List<Disease>> {
        return diseaseRepository.getDiseasesOffline()
    }
}
