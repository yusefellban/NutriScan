package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.api.UserApiService
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

import iti.grad.nutriscan.data.remote.dto.UserDto

class UserRemoteDataSourceImpl @Inject constructor(
    private val userApiService: UserApiService
) : IUserRemoteDataSource {

    override suspend fun getProfile(): UserDto {
        return userApiService.getProfile()
    }

    override suspend fun updateProfile(request: UpdateUserProfileRequestDto): Response<ResponseBody> {
        return userApiService.updateProfile(request)
    }

    override suspend fun uploadProfileImage(image: MultipartBody.Part): UserDto {
        return userApiService.uploadProfileImage(image)
    }

    override suspend fun updateDailyStreak(): Response<Unit> {
        return userApiService.updateDailyStreak()
    }
}
