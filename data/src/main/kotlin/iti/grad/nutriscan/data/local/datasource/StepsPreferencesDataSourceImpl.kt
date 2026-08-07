package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StepsPreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : IStepsPreferencesDataSource {

    private object PreferencesKeys {
        val BASELINE_DATE = stringPreferencesKey("steps_baseline_date")
        val BASELINE_STEPS = floatPreferencesKey("steps_baseline_steps")
        val DAILY_STEPS = intPreferencesKey("steps_daily_total")
        val DAILY_DATE = stringPreferencesKey("steps_daily_date")
    }

    override suspend fun getBaselineDate(): String? {
        return dataStore.data.map { it[PreferencesKeys.BASELINE_DATE] }.first()
    }

    override suspend fun getBaselineSteps(): Float {
        return dataStore.data.map { it[PreferencesKeys.BASELINE_STEPS] ?: 0f }.first()
    }

    override suspend fun saveBaseline(date: String, steps: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASELINE_DATE] = date
            preferences[PreferencesKeys.BASELINE_STEPS] = steps
        }
    }

    override suspend fun getDailySteps(date: String): Int {
        return dataStore.data.map { preferences ->
            if (preferences[PreferencesKeys.DAILY_DATE] == date) {
                preferences[PreferencesKeys.DAILY_STEPS] ?: 0
            } else {
                0
            }
        }.first()
    }

    override suspend fun saveDailySteps(date: String, steps: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_DATE] = date
            preferences[PreferencesKeys.DAILY_STEPS] = steps
        }
    }
}
