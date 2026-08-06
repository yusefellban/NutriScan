package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import javax.inject.Inject

/**
 * Marks a single notification as read.
 *
 * Called when the user taps on a notification history item.
 */
class MarkNotificationReadUseCase @Inject constructor(
    private val repository: INotificationHistoryRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.markAsRead(id)
    }
}
