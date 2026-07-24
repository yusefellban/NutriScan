package iti.grad.nutriscan.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

class NotificationPreferencesDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : INotificationPreferencesDataSource {

    private object PreferencesKeys {
        fun enabledKey(type: NotificationType) = booleanPreferencesKey("notif_enabled_${type.name}")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("notif_quiet_hours_enabled")
        val QUIET_START = stringPreferencesKey("notif_quiet_start")
        val QUIET_END = stringPreferencesKey("notif_quiet_end")
    }

    override fun getPrefs(): Flow<NotificationPrefs> = dataStore.data.map { preferences ->
        NotificationPrefs(
            enabled = NotificationType.entries.associateWith { type ->
                preferences[PreferencesKeys.enabledKey(type)] ?: true
            },
            quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: true,
            quietHoursStart = preferences[PreferencesKeys.QUIET_START]
                ?.let(LocalTime::parse) ?: NotificationPrefs.DEFAULT_QUIET_START,
            quietHoursEnd = preferences[PreferencesKeys.QUIET_END]
                ?.let(LocalTime::parse) ?: NotificationPrefs.DEFAULT_QUIET_END,
        )
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled }
    }

    override suspend fun setEnabled(type: NotificationType, enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.enabledKey(type)] = enabled }
    }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_START] = start.toString()
            preferences[PreferencesKeys.QUIET_END] = end.toString()
        }
    }
}
