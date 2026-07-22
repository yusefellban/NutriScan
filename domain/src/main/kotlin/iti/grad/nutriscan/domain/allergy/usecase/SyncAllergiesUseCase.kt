package iti.grad.nutriscan.domain.allergy.usecase

import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import javax.inject.Inject

class SyncAllergiesUseCase @Inject constructor(
    private val allergyRepository: IAllergyRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return allergyRepository.syncAllergies()
    }
}
