package iti.grad.nutriscan.domain.allergy.repository

import iti.grad.nutriscan.domain.allergy.model.Allergy

interface IAllergyRepository {
    suspend fun getAllergies(): Result<List<Allergy>>
}
