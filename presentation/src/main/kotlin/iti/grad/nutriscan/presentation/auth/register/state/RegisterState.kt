package iti.grad.nutriscan.presentation.auth.register.state

import iti.grad.nutriscan.presentation.common.state.AuthAlertState.None
import iti.grad.nutriscan.presentation.common.state.AuthAlertState
import androidx.annotation.StringRes

data class RegisterState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    @StringRes val firstNameErrorResId: Int? = null,
    @StringRes val lastNameErrorResId: Int? = null,
    @StringRes val emailErrorResId: Int? = null,
    @StringRes val passwordErrorResId: Int? = null,
    @StringRes val confirmPasswordErrorResId: Int? = null,
    val alertState: AuthAlertState = None
)
