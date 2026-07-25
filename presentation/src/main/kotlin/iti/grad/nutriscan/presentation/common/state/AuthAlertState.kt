package iti.grad.nutriscan.presentation.common.state

import iti.grad.nutriscan.presentation.common.model.UiText

sealed class AuthAlertState {
    data object None : AuthAlertState()
    data object InternetError : AuthAlertState()
    data class Error(val message: UiText) : AuthAlertState()
    data class Warning(val message: UiText) : AuthAlertState()
    data class Success(val message: UiText) : AuthAlertState()
}
