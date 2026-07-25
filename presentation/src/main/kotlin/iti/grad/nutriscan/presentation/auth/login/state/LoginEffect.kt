package iti.grad.nutriscan.presentation.auth.login.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToRegister : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
    data class LaunchGoogleLogin(val config: OidcAuthConfig) : LoginEffect
}
