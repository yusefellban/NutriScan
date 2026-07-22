package iti.grad.nutriscan.data.remote.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TokenRefreshApiService {

    @FormUrlEncoded
    @POST("realms/nutriscan/protocol/openid-connect/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String = "mobile-api",
        @Field("refresh_token") refreshToken: String
    ): Response<TokenRefreshResponse>

    @FormUrlEncoded
    @POST("realms/nutriscan/protocol/openid-connect/logout")
    suspend fun logout(
        @Field("client_id") clientId: String = "mobile-api",
        @Field("refresh_token") refreshToken: String
    ): Response<Unit>
}

@kotlinx.serialization.Serializable
data class TokenRefreshResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int
)
