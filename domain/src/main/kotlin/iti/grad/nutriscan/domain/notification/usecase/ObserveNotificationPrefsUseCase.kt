package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotificationPrefsUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    operator fun invoke(): Flow<NotificationPrefs> = notificationRepository.observePrefs()
}
