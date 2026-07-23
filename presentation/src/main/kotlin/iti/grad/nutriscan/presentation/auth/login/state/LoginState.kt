package iti.grad.nutriscan.presentation.auth.login.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.domain.auth.model.OidcAuthConfig

data class LoginState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val emailErrorResId: Int? = null,
    @StringRes val passwordErrorResId: Int? = null,
    val oidcAuthConfig: OidcAuthConfig? = null,
    val alertState: iti.grad.nutriscan.presentation.common.state.AuthAlertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None
)
