package iti.grad.nutriscan.presentation.auth.forgot_password.state

import androidx.annotation.StringRes

data class ForgotPasswordState(
    val selectedMethod: ResetMethod = ResetMethod.EMAIL,
    val isLoading: Boolean = false,
    val showPasswordSentDialog: Boolean = false,
    val maskedEmail: String = ""
)

enum class ResetMethod {
    EMAIL, TWO_FA, GOOGLE_AUTH, SMS
}
