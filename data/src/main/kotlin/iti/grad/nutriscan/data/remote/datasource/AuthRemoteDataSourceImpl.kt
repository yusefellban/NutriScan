package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.api.AuthApiService
import iti.grad.nutriscan.data.remote.dto.RegisterRequestDto
import iti.grad.nutriscan.data.remote.dto.RegisterResponseDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationRequestDto
import iti.grad.nutriscan.data.remote.dto.ResendVerificationResponseDto
import retrofit2.Response
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authApiService: AuthApiService
) : IAuthRemoteDataSource {

    override suspend fun register(
        request: RegisterRequestDto
    ): Response<RegisterResponseDto> {
        return authApiService.register(request)
    }

    override suspend fun resendVerification(
        request: ResendVerificationRequestDto
    ): Response<ResendVerificationResponseDto> {
        return authApiService.resendVerification(request)
    }
}
