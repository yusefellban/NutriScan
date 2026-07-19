package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemePreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : IThemePreferencesDataSource {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override suspend fun getThemeMode(): String {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
        }.first()
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }
}
