package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationResponseDto
import retrofit2.Response

interface IAuthRemoteDataSource {

    suspend fun register(
        request: RegisterRequestDto
    ): Response<RegisterResponseDto>

    suspend fun resendVerification(
        request: ResendVerificationRequestDto
    ): Response<ResendVerificationResponseDto>
}
