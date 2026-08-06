package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.AccountDeletionResponseDto
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import iti.grad.nutriscan.data.remote.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface UserApiService {

    @GET("v1/users/profile")
    suspend fun getProfile(): UserDto

    @PATCH("v1/users/profile")
    suspend fun updateProfile(
        @Body request: UpdateUserProfileRequestDto
    ): Response<ResponseBody>

    /** Uploads a new avatar image. Returns the full, updated user profile. */
    @Multipart
    @POST("v1/users/profile/image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): UserDto

    @POST("v1/users/me/daily-streak")
    suspend fun updateDailyStreak(): Response<Unit>

    /** Schedules the account for permanent deletion. Returns scheduled date + grace period. */
    @DELETE("v1/users/profile")
    suspend fun deleteAccount(): AccountDeletionResponseDto

    /** Cancels a pending deletion and restores the account within the grace period. */
    @POST("v1/users/profile/restore")
    suspend fun restoreAccount(): Response<Unit>
}
