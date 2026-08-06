package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import javax.inject.Inject

/**
 * Deletes a single notification from the history by its [id].
 *
 * Used by the swipe-to-dismiss gesture on the Notification History screen.
 */
class DeleteNotificationUseCase @Inject constructor(
    private val repository: INotificationHistoryRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteNotification(id)
    }
}
