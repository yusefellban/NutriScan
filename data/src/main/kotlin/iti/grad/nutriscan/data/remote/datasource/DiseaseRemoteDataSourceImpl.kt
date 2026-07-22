package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.api.DiseaseApiService
import iti.grad.nutriscan.data.remote.dto.DiseaseDto
import retrofit2.Response
import javax.inject.Inject

class DiseaseRemoteDataSourceImpl @Inject constructor(
    private val diseaseApiService: DiseaseApiService
) : IDiseaseRemoteDataSource {

    override suspend fun getDiseases(): Response<List<DiseaseDto>> {
        return diseaseApiService.getDiseases()
    }
}
