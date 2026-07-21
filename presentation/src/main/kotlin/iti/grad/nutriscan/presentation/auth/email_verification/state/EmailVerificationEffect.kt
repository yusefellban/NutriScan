package iti.grad.nutriscan.presentation.auth.email_verification.state

import androidx.annotation.StringRes

sealed interface EmailVerificationEffect {
    data object NavigateToSignIn : EmailVerificationEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : EmailVerificationEffect
}
