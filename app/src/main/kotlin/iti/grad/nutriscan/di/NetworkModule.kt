package iti.grad.nutriscan.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.BuildConfig
import iti.grad.nutriscan.data.remote.api.AllergyApiService
import iti.grad.nutriscan.data.remote.api.AuthApiService
import iti.grad.nutriscan.data.remote.api.DiseaseApiService
import iti.grad.nutriscan.data.remote.api.KeycloakApiService
import iti.grad.nutriscan.data.remote.api.UserApiService
import iti.grad.nutriscan.data.remote.interceptor.AuthInterceptor
import iti.grad.nutriscan.data.remote.interceptor.ErrorInterceptor
import iti.grad.nutriscan.data.remote.interceptor.NutriScanAuthenticator
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthTokenProvider(): iti.grad.nutriscan.data.remote.interceptor.IAuthTokenProvider {
        return object : iti.grad.nutriscan.data.remote.interceptor.IAuthTokenProvider {
            override suspend fun getToken(): String? = null
        }
    }


    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
        authenticator: NutriScanAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)
            .authenticator(authenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.NUTRISCAN_BASE_URL) 
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
      @Provides
    @Singleton 
   fun provideOpenFoodFactsApiService(retrofit: Retrofit): OpenFoodFactsApiService {
        return retrofit.create(OpenFoodFactsApiService::class.java) }
    @Provides
    @Singleton 
    @javax.inject.Named("KeycloakRetrofit")
    fun provideKeycloakRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.KEYCLOAK_BASE_URL) 
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDiseaseApiService(retrofit: Retrofit): DiseaseApiService {
        return retrofit.create(DiseaseApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAllergyApiService(retrofit: Retrofit): AllergyApiService {
        return retrofit.create(AllergyApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKeycloakApiService(
        @javax.inject.Named("KeycloakRetrofit") retrofit: Retrofit
    ): KeycloakApiService {
        return retrofit.create(KeycloakApiService::class.java)
    }

    @Provides
    @Singleton
    @javax.inject.Named("TokenRefreshRetrofit")
    fun provideTokenRefreshRetrofit(json: Json): Retrofit {
        val okHttpClient = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.KEYCLOAK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenRefreshApiService(@javax.inject.Named("TokenRefreshRetrofit") retrofit: Retrofit): TokenRefreshApiService {
        return retrofit.create(TokenRefreshApiService::class.java)
    }
}
