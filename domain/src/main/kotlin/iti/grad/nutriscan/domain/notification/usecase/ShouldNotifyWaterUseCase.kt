package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.water.model.WaterLog
import java.time.LocalTime
import javax.inject.Inject

class ShouldNotifyWaterUseCase @Inject constructor(
    private val isWithinQuietHours: IsWithinQuietHoursUseCase
) {
    operator fun invoke(prefs: NotificationPrefs, water: WaterLog, now: LocalTime): Boolean {
        if (!prefs.isEnabled(NotificationType.WATER)) return false
        if (isWithinQuietHours(prefs, now)) return false
        return water.glassCount < water.goalGlasses
    }
}
