package iti.grad.nutriscan.domain.disease.repository

import iti.grad.nutriscan.domain.disease.model.Disease

interface IDiseaseRepository {
    suspend fun getDiseases(): Result<List<Disease>>
}
