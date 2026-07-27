package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface INotificationRepository {
    fun observePrefs(): Flow<NotificationPrefs>
    suspend fun setEnabled(type: NotificationType, enabled: Boolean): Result<Unit>
    suspend fun setQuietHours(start: LocalTime, end: LocalTime): Result<Unit>
    suspend fun setQuietHoursEnabled(enabled: Boolean): Result<Unit>
}
