package iti.grad.nutriscan.presentation.auth.register.state

import androidx.annotation.StringRes

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    @StringRes val emailErrorResId: Int? = null,
    @StringRes val passwordErrorResId: Int? = null,
    @StringRes val confirmPasswordErrorResId: Int? = null,
    val alertState: iti.grad.nutriscan.presentation.common.state.AuthAlertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None
)
