package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import okhttp3.ResponseBody
import retrofit2.Response

interface IUserRemoteDataSource {
    suspend fun updateProfile(request: UpdateUserProfileRequestDto): Response<ResponseBody>
}
