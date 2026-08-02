package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.NotificationHistoryDao
import iti.grad.nutriscan.data.db.entity.NotificationHistoryEntity
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import javax.inject.Inject

/**
 * Records each dispatched notification into the local Room history.
 *
 * Injected into every notification worker so they only need to call
 * [record] after posting — keeps the persistence concern out of the workers.
 */
class NotificationHistoryRecorderImpl @Inject constructor(
    private val dao: NotificationHistoryDao,
) : INotificationHistoryRecorder {

    override suspend fun record(type: NotificationType, title: String, body: String) {
        dao.insert(
            NotificationHistoryEntity(
                title = title,
                body = body,
                type = type.name,
                timestamp = System.currentTimeMillis(),
            )
        )
    }
}
