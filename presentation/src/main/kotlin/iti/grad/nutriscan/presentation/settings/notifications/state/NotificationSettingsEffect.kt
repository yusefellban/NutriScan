package iti.grad.nutriscan.presentation.settings.notifications.state

sealed interface NotificationSettingsEffect {
    data object NavigateBack : NotificationSettingsEffect
    data object TestNotificationSent : NotificationSettingsEffect
}
