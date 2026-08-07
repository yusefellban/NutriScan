package iti.grad.nutriscan.presentation.auth.email_verification.state

sealed interface EmailVerificationEvent {
    data object GoToSignInClicked : EmailVerificationEvent
    data object ResendEmailClicked : EmailVerificationEvent
    data object DismissAlert : EmailVerificationEvent
}
