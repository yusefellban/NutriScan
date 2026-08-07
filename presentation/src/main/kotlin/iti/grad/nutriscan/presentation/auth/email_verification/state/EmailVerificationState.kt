package iti.grad.nutriscan.presentation.auth.email_verification.state

import iti.grad.nutriscan.presentation.common.state.AuthAlertState

data class EmailVerificationState(
    val email: String = "",
    val isResending: Boolean = false,
    val alertState: AuthAlertState = AuthAlertState.None
)
