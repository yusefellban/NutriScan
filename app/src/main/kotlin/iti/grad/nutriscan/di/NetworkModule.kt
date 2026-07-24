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
import iti.grad.nutriscan.data.remote.api.ExercisesApiService
import iti.grad.nutriscan.data.remote.api.NewsApiService
import iti.grad.nutriscan.data.remote.api.UserApiService
import iti.grad.nutriscan.data.remote.interceptor.AuthInterceptor
import iti.grad.nutriscan.data.remote.interceptor.ErrorInterceptor
import iti.grad.nutriscan.data.remote.interceptor.NutriScanAuthenticator
import iti.grad.nutriscan.data.remote.api.TokenRefreshApiService
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
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

    @Provides
    @Singleton
    @Named("NewsOkHttpClient")
    fun provideNewsOkHttpClient(): OkHttpClient {
        // Deliberately its own client: the shared AuthInterceptor would attach our
        // backend's Bearer token to a third-party public API, and ErrorInterceptor's
        // 422 -> OcrLowConfidenceException mapping doesn't apply to newsapi.org.
        val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val urlWithKey = original.url.newBuilder()
                .addQueryParameter("apiKey", BuildConfig.NEWS_API_KEY)
                .build()
            chain.proceed(original.newBuilder().url(urlWithKey).build())
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("NewsRetrofit")
    fun provideNewsRetrofit(
        @Named("NewsOkHttpClient") client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.NEWS_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideNewsApiService(@Named("NewsRetrofit") retrofit: Retrofit): NewsApiService =
        retrofit.create(NewsApiService::class.java)

    @Provides
    @Singleton
    @Named("ExercisesOkHttpClient")
    fun provideExercisesOkHttpClient(): OkHttpClient {
        // Deliberately its own client, same reasoning as News: this public API needs no
        // AuthInterceptor (no auth, no API key) and no ErrorInterceptor mapping.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("ExercisesRetrofit")
    fun provideExercisesRetrofit(
        @Named("ExercisesOkHttpClient") client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.EXERCISES_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideExercisesApiService(@Named("ExercisesRetrofit") retrofit: Retrofit): ExercisesApiService =
        retrofit.create(ExercisesApiService::class.java)
}
