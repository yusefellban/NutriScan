package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.DiseaseDto
import retrofit2.Response

interface IDiseaseRemoteDataSource {
    suspend fun getDiseases(): Response<List<DiseaseDto>>
}
