package iti.grad.nutriscan.presentation.account_deletion.state

sealed interface AccountPendingDeletionEffect {
    /** Restoration succeeded — navigate back to the main app flow. */
    data object NavigateToHome : AccountPendingDeletionEffect
    /** User chose to log out from this screen. */
    data object NavigateToLogin : AccountPendingDeletionEffect
}
