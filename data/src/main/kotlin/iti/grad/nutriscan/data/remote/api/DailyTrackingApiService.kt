package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.DailyTrackingMealRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingRequestDto
import iti.grad.nutriscan.data.remote.dto.DailyTrackingResponseDto
import iti.grad.nutriscan.data.remote.dto.PageDailyTrackingSummaryResponseDto
import iti.grad.nutriscan.data.remote.dto.UpdateMealRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DailyTrackingApiService {

    @GET("api/v1/daily-tracking/today")
    suspend fun getToday(): DailyTrackingResponseDto

    @GET("api/v1/daily-tracking/{date}")
    suspend fun getByDate(@Path("date") date: String): DailyTrackingResponseDto

    @GET("api/v1/daily-tracking")
    suspend fun getHistoryPage(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDailyTrackingSummaryResponseDto

    @PATCH("api/v1/daily-tracking/{date}")
    suspend fun updateDay(
        @Path("date") date: String,
        @Body body: DailyTrackingRequestDto,
    ): DailyTrackingResponseDto

    @DELETE("api/v1/daily-tracking/{date}")
    suspend fun deleteDay(@Path("date") date: String)

    @POST("api/v1/daily-tracking/{date}/meals")
    suspend fun addMeal(
        @Path("date") date: String,
        @Body body: DailyTrackingMealRequestDto,
    )

    @PUT("api/v1/daily-tracking/{date}/meals/{scanId}")
    suspend fun updateMeal(
        @Path("date") date: String,
        @Path("scanId") scanId: String,
        @Body body: UpdateMealRequestDto,
    )

    @DELETE("api/v1/daily-tracking/{date}/meals/{scanId}")
    suspend fun deleteMeal(
        @Path("date") date: String,
        @Path("scanId") scanId: String,
    )
}
