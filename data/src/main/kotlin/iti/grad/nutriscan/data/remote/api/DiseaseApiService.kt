package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.DiseaseDto
import retrofit2.Response
import retrofit2.http.GET

interface DiseaseApiService {

    @GET("v1/diseases")
    suspend fun getDiseases(): Response<List<DiseaseDto>>
}
