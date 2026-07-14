package iti.grad.nutriscan.presentation.auth.register

import androidx.annotation.StringRes

sealed interface RegisterEffect {
    data object NavigateToHome : RegisterEffect
    data object NavigateToSignIn : RegisterEffect
    data class ShowSnackbar(@StringRes val messageResId: Int) : RegisterEffect
}
