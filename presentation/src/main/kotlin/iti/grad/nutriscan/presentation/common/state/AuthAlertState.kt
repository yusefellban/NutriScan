package iti.grad.nutriscan.presentation.common.state

sealed class AuthAlertState {
    data object None : AuthAlertState()
    data object InternetError : AuthAlertState()
    data class Error(val messageStr: String? = null, @androidx.annotation.StringRes val messageResId: Int? = null) : AuthAlertState()
    data class Warning(val messageStr: String? = null, @androidx.annotation.StringRes val messageResId: Int? = null) : AuthAlertState()
    data class Success(val messageStr: String? = null, @androidx.annotation.StringRes val messageResId: Int? = null) : AuthAlertState()
}
