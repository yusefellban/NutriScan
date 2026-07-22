package iti.grad.nutriscan.presentation.auth.register.state

import androidx.annotation.StringRes

sealed interface RegisterEffect {
    data class NavigateToEmailVerification(val email: String) : RegisterEffect
    data object NavigateToSignIn : RegisterEffect
    data class ShowErrorDialog(val messageStr: String) : RegisterEffect
}
