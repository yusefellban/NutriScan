package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyStreakUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(
        prefs: NotificationPrefs,
        loggedFoodToday: Boolean,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STREAK)) return false
        if (isWithinQuietHours(prefs, now)) return false
        if (loggedFoodToday) return false
        return now >= EVENING_WARNING_START
    }

    private companion object {
        val EVENING_WARNING_START: LocalTime = LocalTime.of(19, 0)
    }
}
