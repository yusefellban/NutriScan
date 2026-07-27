package iti.grad.nutriscan.data.local.datasource

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface INotificationPreferencesDataSource {
    fun getPrefs(): Flow<NotificationPrefs>
    suspend fun setEnabled(type: NotificationType, enabled: Boolean)
    suspend fun setQuietHours(start: LocalTime, end: LocalTime)
    suspend fun setQuietHoursEnabled(enabled: Boolean)
}
