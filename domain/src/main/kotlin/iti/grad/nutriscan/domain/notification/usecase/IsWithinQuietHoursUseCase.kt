package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import java.time.LocalTime
import javax.inject.Inject

class IsWithinQuietHoursUseCase @Inject constructor() {
    operator fun invoke(prefs: NotificationPrefs, now: LocalTime): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val start = prefs.quietHoursStart
        val end = prefs.quietHoursEnd
        return if (start <= end) {
            now >= start && now < end
        } else {
            // overnight window, e.g. 22:00 - 07:00
            now >= start || now < end
        }
    }
}
