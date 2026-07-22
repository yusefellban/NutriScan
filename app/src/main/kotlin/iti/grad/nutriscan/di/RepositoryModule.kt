package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.data.repository.OnboardingRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import iti.grad.nutriscan.data.repository.ThemeRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import iti.grad.nutriscan.data.repository.LanguageRepositoryImpl
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.data.repository.AuthRepositoryImpl
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import iti.grad.nutriscan.data.repository.DiseaseRepositoryImpl
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.data.repository.UserRepositoryImpl
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import iti.grad.nutriscan.data.repository.AllergyRepositoryImpl
import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import iti.grad.nutriscan.data.repository.StepsRepositoryImpl
import javax.inject.Singleton
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.data.repository.ScanRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): IOnboardingRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): IThemeRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        impl: LanguageRepositoryImpl
    ): ILanguageRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        impl: ScanRepositoryImpl
    ): IScanRepository
     @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindDiseaseRepository(
        impl: DiseaseRepositoryImpl
    ): IDiseaseRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAllergyRepository(
        impl: AllergyRepositoryImpl
    ): IAllergyRepository

    @Binds
    @Singleton
    abstract fun bindStepsRepository(
        impl: StepsRepositoryImpl
    ): IStepsRepository
}
