package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import javax.inject.Inject

class SetQuietHoursEnabledUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        notificationRepository.setQuietHoursEnabled(enabled)
}
