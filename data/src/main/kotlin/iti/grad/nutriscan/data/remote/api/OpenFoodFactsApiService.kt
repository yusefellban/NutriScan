package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.OpenFoodFactsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Headers

interface OpenFoodFactsApiService {
    @Headers("User-Agent: NutriScan-App - Android - Version 1.0")
    @GET("https://world.openfoodfacts.org/api/v2/product/{barcode}")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponseDto
}
