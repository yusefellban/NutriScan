package iti.grad.nutriscan.domain.disease.usecase

import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import javax.inject.Inject

class GetDiseasesUseCase @Inject constructor(
    private val diseaseRepository: IDiseaseRepository
) {
    suspend operator fun invoke(): Result<List<Disease>> {
        return diseaseRepository.getDiseases()
    }
}
