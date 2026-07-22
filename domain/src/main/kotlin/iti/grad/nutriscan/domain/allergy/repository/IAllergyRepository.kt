package iti.grad.nutriscan.domain.allergy.repository

import iti.grad.nutriscan.domain.allergy.model.Allergy

import kotlinx.coroutines.flow.Flow

interface IAllergyRepository {
    fun getAllergiesOffline(): Flow<List<Allergy>>
    suspend fun syncAllergies(): Result<Unit>
    suspend fun getAllergies(): Result<List<Allergy>>
}
