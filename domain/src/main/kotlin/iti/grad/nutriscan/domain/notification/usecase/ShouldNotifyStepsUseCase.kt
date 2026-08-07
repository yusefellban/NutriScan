package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyStepsUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(
        prefs: NotificationPrefs,
        todaySteps: Int,
        goalSteps: Int,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STEPS)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return todaySteps < goalSteps
    }
}
