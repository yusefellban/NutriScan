package iti.grad.nutriscan.presentation.auth.email_verification.state

data class EmailVerificationState(
    val email: String = "",
    val isResending: Boolean = false
)
