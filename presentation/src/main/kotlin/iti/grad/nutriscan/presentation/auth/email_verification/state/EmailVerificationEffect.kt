package iti.grad.nutriscan.presentation.auth.email_verification.state

import androidx.annotation.StringRes

sealed interface EmailVerificationEffect {
    data object NavigateToSignIn : EmailVerificationEffect
}
