package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.remote.interceptor.IAuthTokenProvider
import iti.grad.nutriscan.data.remote.interceptor.StubAuthTokenProvider
import javax.inject.Singleton

/**
 * Provides the [IAuthTokenProvider] binding for the [AuthInterceptor].
 * Currently uses a stub that returns null (no token).
 * Replace with a real implementation when Login/token storage is integrated.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthTokenModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: StubAuthTokenProvider
    ): IAuthTokenProvider
}
