package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.repository.ITestNotificationSender
import javax.inject.Inject

class SendTestNotificationUseCase @Inject constructor(
    private val sender: ITestNotificationSender
) {
    operator fun invoke() = sender.sendNow()
}
