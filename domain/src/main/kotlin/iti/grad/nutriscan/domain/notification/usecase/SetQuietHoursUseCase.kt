package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import java.time.LocalTime
import javax.inject.Inject

class SetQuietHoursUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository
) {
    suspend operator fun invoke(start: LocalTime, end: LocalTime): Result<Unit> =
        notificationRepository.setQuietHours(start, end)
}
