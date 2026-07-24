package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.CategoriesResponseDto
import iti.grad.nutriscan.data.remote.dto.ExercisesResponseDto
import iti.grad.nutriscan.data.remote.dto.SingleExerciseResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Exercises API — public, no auth, no API key
 * (https://exercises-dataset-mu.vercel.app, see the "Exercises API (Production)"
 * Postman collection). Only the endpoints used by ExercisesScreen and
 * ExerciseWorkoutScreen are exposed; `/batch`, `/random`, `/suggestions`, `/stats`,
 * `/equipments`, `/targets` and image/gif proxying are out of scope for now.
 */
interface ExercisesApiService {
    @GET("exercises")
    suspend fun getExercises(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("body_part") bodyPart: String?,
        @Query("q") q: String?,
    ): ExercisesResponseDto

    @GET("exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: String): SingleExerciseResponseDto

    @GET("exercises/categories")
    suspend fun getCategories(): CategoriesResponseDto
}
