package iti.grad.nutriscan.presentation.auth.forgot_password.state

import androidx.annotation.StringRes

data class ForgotPasswordState(
    val selectedMethod: ResetMethod = ResetMethod.EMAIL,
    val email: String = "",
    val emailErrorResId: Int? = null,
    val isLoading: Boolean = false,
    val showEmailInputDialog: Boolean = false,
    val showPasswordSentDialog: Boolean = false,
    val maskedEmail: String = ""
)

enum class ResetMethod {
    EMAIL, TWO_FA, GOOGLE_AUTH, SMS
}
