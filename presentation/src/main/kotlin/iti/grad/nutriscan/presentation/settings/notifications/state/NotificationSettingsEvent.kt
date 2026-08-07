package iti.grad.nutriscan.presentation.settings.notifications.state

import iti.grad.nutriscan.domain.notification.model.NotificationType
import java.time.LocalTime

sealed interface NotificationSettingsEvent {
    data class ToggleType(val type: NotificationType, val enabled: Boolean) : NotificationSettingsEvent
    data class QuietHoursEnabledChanged(val enabled: Boolean) : NotificationSettingsEvent
    data class QuietHoursRangeChanged(val start: LocalTime, val end: LocalTime) : NotificationSettingsEvent
    data object SendTestNotificationClicked : NotificationSettingsEvent
    data object AllowBackgroundNotificationsClicked : NotificationSettingsEvent
    data object BackClicked : NotificationSettingsEvent
}
