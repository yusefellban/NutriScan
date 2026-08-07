package iti.grad.nutriscan.presentation.auth.forgot_password.state

import androidx.annotation.StringRes

sealed interface ForgotPasswordEffect {
    data object NavigateBack : ForgotPasswordEffect
    data class ShowErrorDialog(val messageStr: String) : ForgotPasswordEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ForgotPasswordEffect
}
