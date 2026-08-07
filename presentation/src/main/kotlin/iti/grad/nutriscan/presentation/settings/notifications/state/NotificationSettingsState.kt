package iti.grad.nutriscan.presentation.settings.notifications.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime

@Immutable
data class NotificationSettingsState(
    val enabled: Map<NotificationType, Boolean> = NotificationType.entries.associateWith { true },
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),
    val isIgnoringBatteryOptimizations: Boolean = true,
)
