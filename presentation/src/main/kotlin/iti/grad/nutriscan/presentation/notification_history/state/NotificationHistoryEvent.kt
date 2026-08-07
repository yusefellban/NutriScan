package iti.grad.nutriscan.presentation.notification_history.state

sealed interface NotificationHistoryEvent {
    data object LoadHistory : NotificationHistoryEvent
    data class DeleteNotification(val id: Long) : NotificationHistoryEvent
    data object ClearAll : NotificationHistoryEvent
    data class NotificationClicked(val id: Long) : NotificationHistoryEvent
    data object BackClicked : NotificationHistoryEvent
    data object NavigateToSettingsClicked : NotificationHistoryEvent
}
