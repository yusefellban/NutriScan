package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.AllergyDto
import retrofit2.Response
import retrofit2.http.GET

interface AllergyApiService {

    @GET("v1/allergies")
    suspend fun getAllergies(): Response<List<AllergyDto>>
}
