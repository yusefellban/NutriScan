package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<RegisterResponseDto>

    @POST("v1/auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequestDto
    ): Response<ResendVerificationResponseDto>
}
