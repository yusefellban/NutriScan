package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.remote.interceptor.AuthTokenProviderImpl
import iti.grad.nutriscan.data.remote.interceptor.IAuthTokenProvider
import javax.inject.Singleton

/**
 * Provides the [IAuthTokenProvider] binding for the [AuthInterceptor].
 * Uses AuthTokenProviderImpl which reads from DataStore and handles auto-refresh via AppAuth.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthTokenModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: AuthTokenProviderImpl
    ): IAuthTokenProvider
}
