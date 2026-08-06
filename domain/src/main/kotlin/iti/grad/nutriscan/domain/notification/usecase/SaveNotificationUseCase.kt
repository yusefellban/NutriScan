package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationHistoryItem
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import javax.inject.Inject

/**
 * Persists a single notification into the local history.
 *
 * Called by the notification recording layer after a notification is posted.
 */
class SaveNotificationUseCase @Inject constructor(
    private val repository: INotificationHistoryRepository,
) {
    suspend operator fun invoke(item: NotificationHistoryItem) {
        repository.saveNotification(item)
    }
}
