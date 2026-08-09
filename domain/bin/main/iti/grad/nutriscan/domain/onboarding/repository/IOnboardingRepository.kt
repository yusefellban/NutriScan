package iti.grad.nutriscan.domain.onboarding.repository

interface IOnboardingRepository {
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun completeOnboarding()
    suspend fun isProfileSetupCompleted(): Boolean
    suspend fun completeProfileSetup()
}
