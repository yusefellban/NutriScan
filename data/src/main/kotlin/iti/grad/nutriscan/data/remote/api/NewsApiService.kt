package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * NewsAPI.org — matches the "NutriScan News" Postman collection exactly:
 * "Headlines By category" (category=health) and "Headlines by query" (q=... OR ...).
 * The `apiKey` query param is injected by the News-only OkHttpClient, not here
 * (see NetworkModule.kt) — keeps the secret out of every call site.
 */
interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(@Query("category") category: String): NewsResponseDto

    @GET("v2/everything")
    suspend fun searchArticles(@Query("q") query: String): NewsResponseDto
}
