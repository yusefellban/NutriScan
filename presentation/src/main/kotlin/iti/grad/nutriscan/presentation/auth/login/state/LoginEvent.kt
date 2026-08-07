package iti.grad.nutriscan.presentation.auth.login.state

import iti.grad.nutriscan.domain.auth.model.AuthTokens
import iti.grad.nutriscan.presentation.common.model.SocialMediaProvider

sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object SignInClicked : LoginEvent
    data class SocialLoginClicked(val provider: SocialMediaProvider) : LoginEvent
    data class GoogleLoginSuccess(val authTokens: AuthTokens) : LoginEvent
    data class GoogleLoginFailure(val error: String) : LoginEvent
    data object SignUpClicked : LoginEvent
    data object ForgotPasswordClicked : LoginEvent
    data object DismissAlert : LoginEvent
    data object RetryAction : LoginEvent
}
