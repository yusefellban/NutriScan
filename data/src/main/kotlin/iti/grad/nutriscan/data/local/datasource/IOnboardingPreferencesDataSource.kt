package iti.grad.nutriscan.data.local.datasource

interface IOnboardingPreferencesDataSource {
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted()
    suspend fun isProfileSetupCompleted(): Boolean
    suspend fun setProfileSetupCompleted()
    suspend fun clearUserSpecificPreferences()
}
