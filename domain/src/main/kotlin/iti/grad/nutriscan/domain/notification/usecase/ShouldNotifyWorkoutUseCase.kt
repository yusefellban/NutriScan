package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWorkoutUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(prefs: NotificationPrefs, workoutDone: Boolean, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WORKOUT)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return !workoutDone
    }
}
