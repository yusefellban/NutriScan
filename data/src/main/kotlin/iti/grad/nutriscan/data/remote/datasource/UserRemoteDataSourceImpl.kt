package iti.grad.nutriscan.data.remote.datasource

import iti.grad.nutriscan.data.remote.api.UserApiService
import iti.grad.nutriscan.data.remote.dto.UpdateUserProfileRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class UserRemoteDataSourceImpl @Inject constructor(
    private val userApiService: UserApiService
) : IUserRemoteDataSource {

    override suspend fun updateProfile(request: UpdateUserProfileRequestDto): Response<ResponseBody> {
        return userApiService.updateProfile(request)
    }
}
