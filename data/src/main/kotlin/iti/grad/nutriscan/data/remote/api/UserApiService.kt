package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH

interface UserApiService {

    @PATCH("v1/users/profile")
    suspend fun updateProfile(
        @Body request: UpdateUserProfileRequestDto
    ): Response<ResponseBody>
}
