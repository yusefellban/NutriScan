package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.NotificationHistoryDao
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed implementation of [INotificationHistoryRepository].
 *
 * Maps between [NotificationHistoryEntity] (data) and [NotificationHistoryItem] (domain).
 */
class NotificationHistoryRepositoryImpl @Inject constructor(
    private val dao: NotificationHistoryDao,
) : INotificationHistoryRepository {

    override fun getNotifications(): Flow<List<NotificationHistoryItem>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveNotification(item: NotificationHistoryItem) {
        dao.insert(item.toEntity())
    }

    override suspend fun deleteNotification(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAllNotifications() {
        dao.deleteAll()
    }

    override suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    override fun getUnreadCount(): Flow<Int> =
        dao.getUnreadCount()

    // ── Mappers ─────────────────────────────────────────────────────────

    private fun NotificationHistoryEntity.toDomain(): NotificationHistoryItem =
        NotificationHistoryItem(
            id = id,
            title = title,
            body = body,
            type = runCatching { NotificationType.valueOf(type) }
                .getOrDefault(NotificationType.QUOTE),
            timestamp = timestamp,
            isRead = isRead,
        )

    private fun NotificationHistoryItem.toEntity(): NotificationHistoryEntity =
        NotificationHistoryEntity(
            id = id,
            title = title,
            body = body,
            type = type.name,
            timestamp = timestamp,
            isRead = isRead,
        )
}
