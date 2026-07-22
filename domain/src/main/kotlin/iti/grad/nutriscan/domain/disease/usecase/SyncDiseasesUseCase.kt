package iti.grad.nutriscan.domain.disease.usecase

import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import javax.inject.Inject

class SyncDiseasesUseCase @Inject constructor(
    private val diseaseRepository: IDiseaseRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return diseaseRepository.syncDiseases()
    }
}
