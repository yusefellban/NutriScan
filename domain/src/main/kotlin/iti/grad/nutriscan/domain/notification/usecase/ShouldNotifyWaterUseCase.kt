package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWaterUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    /** [goal] of 0 means the user has no water target yet, which reads as "nothing to fall
     * behind on" — no nudge. */
    operator fun invoke(prefs: NotificationPrefs, consumed: Int, goal: Int, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WATER)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return consumed < goal
    }
}
