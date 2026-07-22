# NutriScan Robust Authentication Architecture

Based on the audit of the current codebase, the Refresh Token mechanism and Auto-Login logic are currently incomplete. The `AppAuth` library is being used, but it's storing tokens in plain text in DataStore, and there's no `Authenticator` to handle `401 Unauthorized` errors smoothly for background Retrofit calls. Additionally, the Splash screen doesn't check for active sessions.

Here is the complete architecture and code implementation to replace this with a secure, robust, and native Retrofit/OkHttp authentication flow.

---

## 1. Secure Storage Management (`EncryptedSharedPreferences`)
First, we add the dependency `androidx.security:security-crypto:1.1.0-alpha06` to `data/build.gradle.kts`.

### `TokenManager.kt`
We create a `TokenManager` class that wraps `EncryptedSharedPreferences` to securely store the `access_token` and `refresh_token`.

```kotlin
package iti.grad.nutriscan.data.local.datasource

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "nutriscan_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString("ACCESS_TOKEN", accessToken)
            .putString("REFRESH_TOKEN", refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = sharedPreferences.getString("ACCESS_TOKEN", null)
    
    fun getRefreshToken(): String? = sharedPreferences.getString("REFRESH_TOKEN", null)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
}
```

---

## 2. OkHttp Interceptor & Authenticator

### `AuthInterceptor.kt`
Automatically attaches the Access Token to every outgoing request.

```kotlin
package iti.grad.nutriscan.data.remote.interceptor

import iti.grad.nutriscan.data.local.datasource.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")

        val token = tokenManager.getAccessToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
```

### `TokenRefreshApiService.kt`
We need a dedicated Retrofit service interface for refreshing tokens and logging out that does **NOT** use the `Authenticator` (to prevent infinite 401 loops).

```kotlin
package iti.grad.nutriscan.data.remote.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// Note: Ensure this uses a Retrofit instance without the NutriScanAuthenticator
interface TokenRefreshApiService {

    @FormUrlEncoded
    @POST("realms/nutriscan/protocol/openid-connect/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String = "mobile_api",
        @Field("refresh_token") refreshToken: String
    ): Response<TokenRefreshResponse>

    @FormUrlEncoded
    @POST("realms/nutriscan/protocol/openid-connect/logout")
    suspend fun logout(
        @Field("client_id") clientId: String = "mobile_api",
        @Field("refresh_token") refreshToken: String
    ): Response<Unit>
}

@kotlinx.serialization.Serializable
data class TokenRefreshResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int
)
```

### `NutriScanAuthenticator.kt`
Handles `401 Unauthorized`. It uses a synchronized block so that if multiple concurrent API calls fail, only the first one triggers the refresh API call, and the others wait and use the new token.

```kotlin
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
    // Use Provider to avoid circular dependency in NetworkModule
    private val tokenRefreshApiService: Provider<TokenRefreshApiService> 
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val currentToken = tokenManager.getAccessToken()

        synchronized(this) {
            val updatedToken = tokenManager.getAccessToken()
            // If the token was refreshed by another thread while this thread was waiting
            if (currentToken != updatedToken && updatedToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $updatedToken")
                    .build()
            }

            // Refresh token is required
            val refreshToken = tokenManager.getRefreshToken() ?: return null

            val refreshResponse = runBlocking {
                try {
                    tokenRefreshApiService.get().refreshToken(refreshToken = refreshToken)
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

            // If refresh failed (e.g., invalid_grant), logout and clear tokens
            runBlocking {
                try {
                    tokenRefreshApiService.get().logout(refreshToken = refreshToken)
                } catch (e: Exception) { /* Ignore logout errors */ }
            }
            tokenManager.clearTokens()
            
            // To force navigation to Login, you can either use an EventBus or throw a specific exception
            // that gets caught by an ErrorInterceptor to trigger UI navigation.
            return null
        }
    }
}
```

### Updates to `NetworkModule.kt`
```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    authInterceptor: AuthInterceptor,
    errorInterceptor: ErrorInterceptor,
    authenticator: NutriScanAuthenticator
): OkHttpClient {
    // ...
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(errorInterceptor)
        .authenticator(authenticator)
        // ...
        .build()
}

// Dedicated Retrofit for Token Refresh (NO Authenticator added)
@Provides
@Singleton
@Named("TokenRefreshRetrofit")
fun provideTokenRefreshRetrofit(json: Json): Retrofit {
    val okHttpClient = OkHttpClient.Builder().build() // Clean client
    return Retrofit.Builder()
        .baseUrl(BuildConfig.KEYCLOAK_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

@Provides
@Singleton
fun provideTokenRefreshApiService(@Named("TokenRefreshRetrofit") retrofit: Retrofit): TokenRefreshApiService {
    return retrofit.create(TokenRefreshApiService::class.java)
}
```

---

## 3. Auto-Login / App Initialization (Splash)

We update `SplashViewModel` to check the `TokenManager` directly on launch.

```kotlin
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase,
    private val tokenManager: TokenManager
) : ViewModel() {

    // ...
    private fun onAnimationCompleted() {
        viewModelScope.launch {
            if (isOnboardingCompletedUseCase()) {
                val accessToken = tokenManager.getAccessToken()
                if (!accessToken.isNullOrBlank()) {
                    _effect.send(SplashEffect.NavigateToHome)
                } else {
                    _effect.send(SplashEffect.NavigateToLogin)
                }
            } else {
                _effect.send(SplashEffect.NavigateToOnboarding)
            }
        }
    }
}
```
