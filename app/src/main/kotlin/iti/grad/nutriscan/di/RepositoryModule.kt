package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.data.repository.OnboardingRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): IOnboardingRepository
}
