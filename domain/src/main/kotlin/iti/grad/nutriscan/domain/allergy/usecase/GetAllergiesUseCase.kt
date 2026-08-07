package iti.grad.nutriscan.domain.allergy.usecase

import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllergiesUseCase @Inject constructor(
    private val allergyRepository: IAllergyRepository
) {
    operator fun invoke(): Flow<List<Allergy>> {
        return allergyRepository.getAllergiesOffline()
    }
}
