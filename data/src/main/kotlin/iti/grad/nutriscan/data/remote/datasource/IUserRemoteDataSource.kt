package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import okhttp3.ResponseBody
import retrofit2.Response

import iti.grad.nutriscan.data.remote.dto.UserDto

interface IUserRemoteDataSource {
    suspend fun getProfile(): UserDto
    suspend fun updateProfile(request: UpdateUserProfileRequestDto): Response<ResponseBody>
}
