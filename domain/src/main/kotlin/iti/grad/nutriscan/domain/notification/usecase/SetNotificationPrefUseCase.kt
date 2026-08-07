package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import javax.inject.Inject

class SetNotificationPrefUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(type: NotificationType, enabled: Boolean): Result<Unit> =
        notificationRepository.setEnabled(type, enabled)
}
