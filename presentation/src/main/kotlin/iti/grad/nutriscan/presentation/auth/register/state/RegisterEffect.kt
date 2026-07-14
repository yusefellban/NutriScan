package iti.grad.nutriscan.presentation.auth.register.state

import androidx.annotation.StringRes

sealed interface RegisterEffect {
    data object NavigateToHome : RegisterEffect
    data object NavigateToSignIn : RegisterEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : RegisterEffect
}
