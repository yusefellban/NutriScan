package iti.grad.nutriscan.presentation.notification_history.state

sealed interface NotificationHistoryEffect {
    data object NavigateBack : NotificationHistoryEffect
    data object NavigateToSettings : NotificationHistoryEffect
    data class ShowUndoSnackbar(val messageResId: Int) : NotificationHistoryEffect
}
