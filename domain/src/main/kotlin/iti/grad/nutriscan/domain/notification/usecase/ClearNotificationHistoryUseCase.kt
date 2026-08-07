package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import javax.inject.Inject

/**
 * Clears the entire notification history.
 *
 * Triggered by the "Clear All" action on the Notification History screen.
 * The UI should show a confirmation dialog before calling this.
 */
class ClearNotificationHistoryUseCase @Inject constructor(
    private val repository: INotificationHistoryRepository,
) {
    suspend operator fun invoke() {
        repository.clearAllNotifications()
    }
}
