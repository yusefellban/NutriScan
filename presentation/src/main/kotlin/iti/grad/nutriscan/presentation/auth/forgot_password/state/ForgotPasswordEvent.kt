package iti.grad.nutriscan.presentation.auth.forgot_password.state

sealed interface ForgotPasswordEvent {
    data class MethodSelected(val method: ResetMethod) : ForgotPasswordEvent
    data object ResetPasswordClicked : ForgotPasswordEvent
    data object BackClicked : ForgotPasswordEvent
    data object ResendCodeClicked : ForgotPasswordEvent
    data object DismissPasswordSentDialog : ForgotPasswordEvent
}
