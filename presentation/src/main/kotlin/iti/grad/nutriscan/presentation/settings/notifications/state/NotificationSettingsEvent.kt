package iti.grad.nutriscan.presentation.settings.notifications.state

import iti.grad.nutriscan.domain.notification.model.NotificationType

sealed interface NotificationSettingsEvent {
    data class ToggleType(val type: NotificationType, val enabled: Boolean) : NotificationSettingsEvent
    data class QuietHoursEnabledChanged(val enabled: Boolean) : NotificationSettingsEvent
    data object SendTestNotificationClicked : NotificationSettingsEvent
    data object AllowBackgroundNotificationsClicked : NotificationSettingsEvent
    data object BackClicked : NotificationSettingsEvent
}
