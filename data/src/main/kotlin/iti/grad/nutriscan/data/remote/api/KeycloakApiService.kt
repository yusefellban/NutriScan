package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.KeycloakTokenResponseDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface KeycloakApiService {

    @FormUrlEncoded
    @POST("realms/nutriscan/protocol/openid-connect/token")
    suspend fun loginWithEmail(
        @Field("grant_type") grantType: String = "password",
        @Field("client_id") clientId: String = "mobile-api",
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<KeycloakTokenResponseDto>
}
