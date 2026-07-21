package iti.grad.nutriscan.presentation.auth.email_verification.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.auth.usecase.ResendVerificationEmailUseCase
import iti.grad.nutriscan.presentation.auth.email_verification.state.EmailVerificationEffect
import iti.grad.nutriscan.presentation.auth.email_verification.state.EmailVerificationEvent
import iti.grad.nutriscan.presentation.auth.email_verification.state.EmailVerificationState
import iti.grad.presentation.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        EmailVerificationState(
            email = savedStateHandle.get<String>("email").orEmpty()
        )
    )
    val state: StateFlow<EmailVerificationState> = _state.asStateFlow()

    private val _effect = Channel<EmailVerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.GoToSignInClicked -> handleGoToSignIn()
            is EmailVerificationEvent.ResendEmailClicked -> handleResendEmail()
        }
    }

    private fun handleGoToSignIn() {
        viewModelScope.launch {
            _effect.send(EmailVerificationEffect.NavigateToSignIn)
        }
    }

    private fun handleResendEmail() {
        val email = _state.value.email
        if (email.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true) }
            resendVerificationEmailUseCase(email)
                .onSuccess {
                    _state.update { it.copy(isResending = false) }
                    _effect.send(
                        EmailVerificationEffect.ShowSnackbar(
                            messageResId = R.string.email_verification_resend_success
                        )
                    )
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isResending = false) }
                    _effect.send(
                        EmailVerificationEffect.ShowSnackbar(
                            messageStr = throwable.message
                                ?: "Failed to resend verification email."
                        )
                    )
                }
        }
    }
}
