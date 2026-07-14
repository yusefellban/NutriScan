package iti.grad.nutriscan.presentation.auth.register

sealed interface RegisterEvent {
    data class EmailChanged(val value: String) : RegisterEvent
    data class PasswordChanged(val value: String) : RegisterEvent
    data class ConfirmPasswordChanged(val value: String) : RegisterEvent
    data object TogglePasswordVisibility : RegisterEvent
    data object ToggleConfirmPasswordVisibility : RegisterEvent
    data object SignUpClicked : RegisterEvent
    data object SignInClicked : RegisterEvent
}
