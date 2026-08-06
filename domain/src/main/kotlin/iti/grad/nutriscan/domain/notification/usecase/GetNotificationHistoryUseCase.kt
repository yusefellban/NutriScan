package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the full notification history, newest first.
 *
 * Returns a [Flow] so the UI automatically updates when new notifications
 * arrive or items are deleted.
 */
class GetNotificationHistoryUseCase @Inject constructor(
    private val repository: INotificationHistoryRepository,
) {
    operator fun invoke(): Flow<List<NotificationHistoryItem>> =
        repository.getNotifications()
}
