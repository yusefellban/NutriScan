package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.AllergyDto
import retrofit2.Response

interface IAllergyRemoteDataSource {
    suspend fun getAllergies(): Response<List<AllergyDto>>
}
