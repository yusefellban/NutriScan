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
        hasStreak: Boolean,
        now: LocalTime,
    ): Boolean {
        if (!prefs.isEnabled(NotificationType.STREAK)) return false
        if (isWithinQuietHours(prefs, now)) return false
        if (loggedFoodToday) return false
        // The Food reminder at 19:30 already covers "you have logged nothing today". This one
        // only earns its place when there is an actual streak on the line — and its copy reads
        // as nonsense at zero.
        if (!hasStreak) return false
        return now >= EVENING_WARNING_START
    }

    private companion object {
        val EVENING_WARNING_START: LocalTime = LocalTime.of(19, 0)
    }
}
