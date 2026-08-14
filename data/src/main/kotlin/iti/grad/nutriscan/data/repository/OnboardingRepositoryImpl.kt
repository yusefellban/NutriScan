package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.data.local.datasource.IOnboardingPreferencesDataSource
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val onboardingDataSource: IOnboardingPreferencesDataSource
) : IOnboardingRepository {

    override suspend fun isOnboardingCompleted(): Boolean {
        return onboardingDataSource.isOnboardingCompleted()
    }

    override suspend fun completeOnboarding() {
        onboardingDataSource.setOnboardingCompleted()
    }

    override suspend fun isProfileSetupCompleted(): Boolean {
        return onboardingDataSource.isProfileSetupCompleted()
    }

    override suspend fun completeProfileSetup() {
        onboardingDataSource.setProfileSetupCompleted()
    }

    override suspend fun clearUserSpecificPreferences() {
        onboardingDataSource.clearUserSpecificPreferences()
    }
}
