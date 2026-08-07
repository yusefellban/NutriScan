package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.api.AllergyApiService
import iti.grad.nutriscan.data.remote.dto.AllergyDto
import retrofit2.Response
import javax.inject.Inject

class AllergyRemoteDataSourceImpl @Inject constructor(
    private val allergyApiService: AllergyApiService
) : IAllergyRemoteDataSource {

    override suspend fun getAllergies(): Response<List<AllergyDto>> {
        return allergyApiService.getAllergies()
    }
}
