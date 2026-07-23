package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.NutriGptRequestDto
import iti.grad.nutriscan.data.remote.dto.NutriGptResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NutriGptApiService {
    @POST("api/query")
    suspend fun sendQuery(
        @Body request: NutriGptRequestDto
    ): NutriGptResponseDto
}
