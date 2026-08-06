package iti.grad.nutriscan.presentation.account_deletion.state

sealed interface AccountPendingDeletionEvent {
    data object RestoreAccountClicked : AccountPendingDeletionEvent
    data object LogoutClicked : AccountPendingDeletionEvent
    data object ErrorDismissed : AccountPendingDeletionEvent
}
