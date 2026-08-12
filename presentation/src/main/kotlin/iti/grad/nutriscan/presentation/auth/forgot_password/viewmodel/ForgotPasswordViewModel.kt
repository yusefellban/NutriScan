package iti.grad.nutriscan.presentation.auth.forgot_password.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEffect
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordEvent
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ForgotPasswordState
import iti.grad.nutriscan.presentation.auth.forgot_password.state.ResetMethod
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import iti.grad.nutriscan.domain.auth.usecase.ForgotPasswordUseCase
import iti.grad.presentation.R
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.MethodSelected -> {
                if (event.method == ResetMethod.EMAIL) {
                    _state.update { it.copy(selectedMethod = event.method) }
                } else {
                    viewModelScope.launch {
                        _effect.send(ForgotPasswordEffect.ShowSnackbar(messageResId = R.string.feature_coming_soon))
                    }
                }
            }
            is ForgotPasswordEvent.EmailChanged -> _state.update {
                it.copy(email = event.email, emailErrorResId = null)
            }
            is ForgotPasswordEvent.ResetPasswordClicked -> _state.update {
                it.copy(showEmailInputDialog = true)
            }
            is ForgotPasswordEvent.DismissEmailInputDialog -> _state.update {
                it.copy(showEmailInputDialog = false)
            }
            is ForgotPasswordEvent.SendResetLink -> handleSendResetLink()
            is ForgotPasswordEvent.BackClicked -> {
                viewModelScope.launch { _effect.send(ForgotPasswordEffect.NavigateBack) }
            }
            is ForgotPasswordEvent.ResendCodeClicked -> handleResendCode()
            is ForgotPasswordEvent.DismissPasswordSentDialog -> _state.update {
                it.copy(showPasswordSentDialog = false)
            }
        }
    }

    private fun handleSendResetLink() {
        val emailToUse = _state.value.email
        
        val emailError = when {
            emailToUse.isBlank() -> R.string.error_empty_field
            !emailToUse.matches(emailRegex) -> R.string.error_invalid_email
            else -> null
        }
        
        _state.update { it.copy(emailErrorResId = emailError) }
        
        if (emailError != null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            forgotPasswordUseCase(emailToUse)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showEmailInputDialog = false,
                            showPasswordSentDialog = true,
                            maskedEmail = maskEmail(emailToUse)
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isLoading = false, showEmailInputDialog = false) }
                    _effect.send(
                        ForgotPasswordEffect.ShowErrorDialog(
                            messageStr = throwable.message ?: "Failed to send reset email"
                        )
                    )
                }
        }
    }

    private fun handleResendCode() {
        val emailToUse = _state.value.email
        if (emailToUse.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            forgotPasswordUseCase(emailToUse)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(
                        ForgotPasswordEffect.ShowSnackbar(messageStr = "Code resent successfully")
                    )
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(
                        ForgotPasswordEffect.ShowErrorDialog(
                            messageStr = throwable.message ?: "Failed to resend reset email"
                        )
                    )
                }
        }
    }

    /**
     * Masks an email address for privacy display.
     * e.g. "elementary221b@gmail.com" → "elem*******221b@gmail.com"
     */
    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 4) return email
        val prefix = email.substring(0, 4)
        val suffix = email.substring(atIndex - 4)
        return "$prefix${"*".repeat(atIndex - 8)}$suffix"
    }
}
