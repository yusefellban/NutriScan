package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationResponseDto
import iti.grad.nutriscan.data.remote.dto.ForgotPasswordRequestDto
import iti.grad.nutriscan.data.remote.dto.MessageResponseDto
import retrofit2.Response

interface IAuthRemoteDataSource {

    suspend fun register(
        request: RegisterRequestDto
    ): Response<RegisterResponseDto>

    suspend fun resendVerification(
        request: ResendVerificationRequestDto
    ): Response<ResendVerificationResponseDto>

    suspend fun forgotPassword(
        request: ForgotPasswordRequestDto
    ): Response<MessageResponseDto>
}
