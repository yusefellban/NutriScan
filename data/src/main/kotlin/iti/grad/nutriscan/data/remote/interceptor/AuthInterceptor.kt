package iti.grad.nutriscan.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

interface IAuthTokenProvider {
    suspend fun getToken(): String?
}

class AuthInterceptor @Inject constructor(
    private val authTokenProvider: IAuthTokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.getToken() }
        
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
