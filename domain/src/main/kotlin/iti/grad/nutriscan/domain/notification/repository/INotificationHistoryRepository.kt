package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for notification history persistence.
 *
 * Reads are reactive [Flow]s so the UI auto-updates when the underlying
 * data changes. Writes are one-shot [suspend] functions.
 */
interface INotificationHistoryRepository {

    /** Observe all notifications, newest first. */
    fun getNotifications(): Flow<List<NotificationHistoryItem>>

    /** Persist a new notification entry. */
    suspend fun saveNotification(item: NotificationHistoryItem)

    /** Delete a single notification by its ID. */
    suspend fun deleteNotification(id: Long)

    /** Delete all notification history. */
    suspend fun clearAllNotifications()

    /** Mark a single notification as read. */
    suspend fun markAsRead(id: Long)

    /** Observe the count of unread notifications. */
    fun getUnreadCount(): Flow<Int>
}
