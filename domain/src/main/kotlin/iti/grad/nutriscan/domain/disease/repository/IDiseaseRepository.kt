package iti.grad.nutriscan.domain.disease.repository

import iti.grad.nutriscan.domain.disease.model.Disease

import kotlinx.coroutines.flow.Flow

interface IDiseaseRepository {
    fun getDiseasesOffline(): Flow<List<Disease>>
    suspend fun syncDiseases(): Result<Unit>
    suspend fun getDiseases(): Result<List<Disease>> // Keeping for backward compatibility or direct fetch
}
