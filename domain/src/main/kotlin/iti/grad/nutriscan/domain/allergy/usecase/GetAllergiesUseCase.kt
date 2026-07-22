package iti.grad.nutriscan.domain.allergy.usecase

import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import javax.inject.Inject

class GetAllergiesUseCase @Inject constructor(
    private val allergyRepository: IAllergyRepository
) {
    suspend operator fun invoke(): Result<List<Allergy>> {
        return allergyRepository.getAllergies()
    }
}
