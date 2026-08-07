package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LanguagePreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ILanguagePreferencesDataSource {

    private object PreferencesKeys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    override suspend fun getLanguage(): String {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.LANGUAGE_CODE] ?: "EN"
        }.first()
    }

    override suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_CODE] = language
        }
    }

    override fun observeLanguage(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.LANGUAGE_CODE] ?: "EN"
        }
    }
}
