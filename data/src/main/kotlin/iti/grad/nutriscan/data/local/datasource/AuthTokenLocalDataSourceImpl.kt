package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthTokenLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : IAuthTokenLocalDataSource {

    private object PreferencesKeys {
        val AUTH_STATE = stringPreferencesKey("auth_state_json")
    }

    override suspend fun saveAuthState(stateJson: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_STATE] = stateJson
        }
    }

    override suspend fun getAuthState(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.AUTH_STATE]
        }.firstOrNull()
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_STATE)
        }
    }
}
