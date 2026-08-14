package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingPreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : IOnboardingPreferencesDataSource {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PROFILE_SETUP_COMPLETED = booleanPreferencesKey("profile_setup_completed")
    }

    override suspend fun isOnboardingCompleted(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }.first()
    }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun isProfileSetupCompleted(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.PROFILE_SETUP_COMPLETED] ?: false
        }.first()
    }

    override suspend fun setProfileSetupCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_SETUP_COMPLETED] = true
        }
    }

    override suspend fun clearUserSpecificPreferences() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PROFILE_SETUP_COMPLETED)
        }
    }
}
