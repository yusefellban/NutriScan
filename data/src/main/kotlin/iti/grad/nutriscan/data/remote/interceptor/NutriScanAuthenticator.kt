package iti.grad.nutriscan.data.remote.interceptor

import iti.grad.nutriscan.data.local.datasource.TokenManager
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class NutriScanAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val tokenRefreshApiServiceProvider: Provider<TokenRefreshApiService> 
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val currentToken = tokenManager.getAccessToken()

        synchronized(this) {
            val updatedToken = tokenManager.getAccessToken()
            // If token refreshed by another thread
            if (currentToken != updatedToken && updatedToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $updatedToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken() ?: return null

            val refreshResponse = runBlocking {
                try {
                    tokenRefreshApiServiceProvider.get().refreshToken(refreshToken = refreshToken)
                } catch (e: Exception) {
                    null
                }
            }

            if (refreshResponse != null && refreshResponse.isSuccessful) {
                val newAccessToken = refreshResponse.body()?.access_token
                val newRefreshToken = refreshResponse.body()?.refresh_token

                if (newAccessToken != null && newRefreshToken != null) {
                    tokenManager.saveTokens(newAccessToken, newRefreshToken)
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                }
            }

            // Refresh failed (invalid_grant), logout and clear tokens
            runBlocking {
                try {
                    tokenRefreshApiServiceProvider.get().logout(refreshToken = refreshToken)
                } catch (e: Exception) { }
            }
            tokenManager.clearTokens()
            return null
        }
    }
}
