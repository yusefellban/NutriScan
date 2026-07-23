package iti.grad.nutriscan.presentation.auth.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.auth.usecase.RegisterUseCase
import iti.grad.presentation.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEffect
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.state.RegisterState

import iti.grad.nutriscan.domain.auth.usecase.ResendVerificationEmailUseCase

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Pure Kotlin Regex to adhere to "No Android Imports in ViewModel" rule
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.EmailChanged -> _state.update { it.copy(email = event.value, emailErrorResId = null, alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None) }
            is RegisterEvent.PasswordChanged -> _state.update { it.copy(password = event.value, passwordErrorResId = null, alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None) }
            is RegisterEvent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null, alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None) }
            is RegisterEvent.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            is RegisterEvent.ToggleConfirmPasswordVisibility -> _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
            is RegisterEvent.SignUpClicked -> handleSignUp()
            is RegisterEvent.SignInClicked -> navigateToSignIn()
            is RegisterEvent.DismissAlert -> {
                val wasSuccess = _state.value.alertState is iti.grad.nutriscan.presentation.common.state.AuthAlertState.Success
                _state.update { it.copy(alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None) }
                if (wasSuccess) {
                    // When the user clicks OK on the success alert, navigate to Login
                    navigateToSignIn()
                }
            }
            is RegisterEvent.RetryAction -> handleSignUp()
        }
    }

    private fun handleSignUp() {
        val currentState = _state.value
        
        val emailError = when {
            currentState.email.isBlank() -> R.string.error_empty_field
            !currentState.email.matches(emailRegex) -> R.string.error_invalid_email
            else -> null
        }
        
        val passwordError = when {
            currentState.password.isBlank() -> R.string.error_empty_field
            else -> null
        }
        
        val confirmPasswordError = when {
            currentState.password != currentState.confirmPassword -> R.string.error_password_mismatch
            else -> null
        }
        
        _state.update { 
            it.copy(
                emailErrorResId = emailError,
                passwordErrorResId = passwordError,
                confirmPasswordErrorResId = confirmPasswordError
            )
        }
        
        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _state.update { it.copy(alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.Warning(messageResId = R.string.error_validation_fields)) }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.None) }
            registerUseCase(currentState.email, currentState.password)
                .onSuccess {
                    resendVerificationEmailUseCase(currentState.email)
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            alertState = iti.grad.nutriscan.presentation.common.state.AuthAlertState.Success(messageResId = R.string.success_account_created)
                        ) 
                    }
                }
                .onFailure { throwable ->
                    val msg = throwable.message.orEmpty()
                    val isConflict = msg.contains("409") || msg.contains("exists")
                    
                    val newAlertState = when {
                        throwable is java.io.IOException -> iti.grad.nutriscan.presentation.common.state.AuthAlertState.InternetError
                        isConflict -> iti.grad.nutriscan.presentation.common.state.AuthAlertState.Warning(messageResId = R.string.error_email_exists)
                        msg.contains("500") || msg.contains("Server Error") -> iti.grad.nutriscan.presentation.common.state.AuthAlertState.Error(messageResId = R.string.error_server_down)
                        else -> iti.grad.nutriscan.presentation.common.state.AuthAlertState.Error(messageStr = throwable.message ?: "Registration failed")
                    }
                    _state.update { it.copy(isLoading = false, alertState = newAlertState) }
                }
        }
    }

    private fun navigateToSignIn() {
        viewModelScope.launch {
            _effect.send(RegisterEffect.NavigateToSignIn)
        }
    }
}
