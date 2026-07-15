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
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.MethodSelected -> _state.update {
                it.copy(selectedMethod = event.method)
            }
            is ForgotPasswordEvent.ResetPasswordClicked -> handleResetPassword()
            is ForgotPasswordEvent.BackClicked -> {
                viewModelScope.launch { _effect.send(ForgotPasswordEffect.NavigateBack) }
            }
            is ForgotPasswordEvent.ResendCodeClicked -> handleResendCode()
            is ForgotPasswordEvent.DismissPasswordSentDialog -> _state.update {
                it.copy(showPasswordSentDialog = false)
            }
        }
    }

    private fun handleResetPassword() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // Simulate API call — will be replaced with actual use-case
            delay(1500)
            _state.update {
                it.copy(
                    isLoading = false,
                    showPasswordSentDialog = true,
                    maskedEmail = maskEmail("elementary221b@gmail.com")
                )
            }
        }
    }

    private fun handleResendCode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(1000)
            _state.update { it.copy(isLoading = false) }
            _effect.send(
                ForgotPasswordEffect.ShowSnackbar(messageStr = "Code resent successfully")
            )
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
